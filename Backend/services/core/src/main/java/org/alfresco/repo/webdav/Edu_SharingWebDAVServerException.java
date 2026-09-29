package org.alfresco.repo.webdav;

import jakarta.servlet.http.HttpServletResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * edu-sharing fix
 *
 * A {@link WebDAVServerException} that additionally carries an MS-WDV extended error, i.e. an error
 * number plus a human readable reason. {@link ExceptionHandler} turns it into the X-MSDAVEXT_ERROR
 * response header ([MS-WDV] section 2.2.3). The Windows WebDAV client translates that number into a
 * Win32 error code and displays its own message for it, so a rejected upload no longer surfaces as
 * "item could not be found" - which is what the blanket 500 this used to answer with produced - but
 * as "the file type has been blocked" or "the file contains a virus".
 *
 * Clients other than Windows ignore the unknown header. For them the improvement is the status code,
 * which is 403/413/507 instead of a 500, so davfs2 reports "permission denied" rather than an
 * unspecific I/O error.
 */
public class Edu_SharingWebDAVServerException extends WebDAVServerException
{
    private static final long serialVersionUID = 1L;

    private static final String HEADER_MSDAVEXT_ERROR = "X-MSDAVEXT_ERROR";

    /**
     * Extended errors that the Windows WebDAV client translates into a Win32 error code, taken from
     * the table in [MS-WDV] section 3.1.5.3 (spelled out in Appendix A note 17). The client maps the
     * numeric part of the header through that fixed table; on a hit the application receives a real
     * Win32 error and Windows shows its own specific message for it. On a miss the spec keeps the
     * string available to the application, but the Explorer copy dialog does not display it - so the
     * exact values below are what makes the difference, and the module code *ranges* listed in
     * [MS-WEBDAVE] section 2.2.3 are of no use here.
     */
    /** V_BAD_FILETYPE_NO_URL, "the server blocked the file because of its type" -> Win32 222 */
    private static final int MSERR_BAD_FILETYPE = 0x0009006F;
    /** V_VIRUS_INFECTED_UL, "the file was infected with a virus and cannot be uploaded" -> Win32 225 */
    private static final int MSERR_VIRUS_INFECTED_UPLOAD = 0x00960004;
    /** V_OVER_QUOTA, "the target site is over its disk quota" -> Win32 1295 */
    private static final int MSERR_OVER_QUOTA = 0x00090063;

    /**
     * That table has no row for a virus scan that could not be carried out, nor for an exceeded file
     * size. These codes are therefore deliberately outside it: claiming V_VIRUS_INFECTED_UL for a
     * failed scan would tell the user the file is infected, which we do not know. Both stay useful -
     * the status code still carries the outcome, and clients that show the string still show it.
     */
    private static final int MSERR_VIRUS_SCAN_FAILED = 0x00960001;
    private static final int MSERR_FILE_SIZE = 0x00020003;

    /**
     * 507 Insufficient Storage, see RFC 4918 section 11.5. Deliberately not
     * CCConstants.HTTP_INSUFFICIENT_STORAGE, which holds 503 (Service Unavailable).
     */
    private static final int SC_INSUFFICIENT_STORAGE = 507;

    /** guards against a cause chain that contains a cycle */
    private static final int MAX_CAUSE_DEPTH = 20;

    private final int extendedErrorCode;
    private final String extendedErrorMessage;

    public Edu_SharingWebDAVServerException(int httpStatusCode, int extendedErrorCode,
                                            String extendedErrorMessage, Throwable cause)
    {
        super(httpStatusCode, cause);
        this.extendedErrorCode = extendedErrorCode;
        this.extendedErrorMessage = extendedErrorMessage;
    }

    /**
     * Adds the extended error as a response header. Has to be called before the response is
     * committed, i.e. before ExceptionHandler writes the status.
     */
    void applyExtendedErrorHeader(HttpServletResponse response)
    {
        if (extendedErrorMessage == null || extendedErrorMessage.isEmpty())
        {
            return;
        }
        // MSError-Header = "X-MSDAVEXT_ERROR" ":" Extended-error "; " Error-string
        // Error-string is percent encoded UTF-8, which besides following the spec keeps text that
        // originates from the request (a detected mimetype, a file name) from breaking out of the
        // header value.
        response.setHeader(HEADER_MSDAVEXT_ERROR,
                extendedErrorCode + "; " + URLEncoder.encode(extendedErrorMessage, StandardCharsets.UTF_8));
    }

    /**
     * Maps the exception a WebDAV method failed with to a status code plus a reason for the client.
     * Returns <tt>null</tt> for anything that is not a known content rejection, so callers keep their
     * existing fallback for those.
     *
     * The reason is deliberately not localized. Where the number maps, Windows displays its own
     * already localized message and never looks at the string; where it does not map, only clients
     * other than the Explorer copy dialog show it at all. A string is nevertheless required to
     * accompany every number, see [MS-WEBDAVE] section 2.2.3.
     *
     * @param e              exception the method failed with, its cause chain is searched
     * @param rejectedStatus status to answer a rejected upload with. Callers pass what
     *                       {@link WebDAVMethod#getStatusForAccessDeniedException()} yields, so that
     *                       macOS clients keep the 500 they are deliberately given there (see the
     *                       accessDeniedStatusCodes mapping in {@link WebDAVMethod}) - Finder
     *                       re-prompts for credentials when a PUT is answered with 403.
     */
    static Edu_SharingWebDAVServerException classify(Throwable e, int rejectedStatus)
    {
        Throwable t = e;
        for (int depth = 0; t != null && depth < MAX_CAUSE_DEPTH; t = t.getCause(), depth++)
        {
            // Matched by class name rather than by instanceof: the policies that raise these run in
            // the Alfresco application context, so an instance can originate from a different class
            // loader, and the virus scan exceptions live in an external module that is not on this
            // webapp's classpath at all. DAOException.mapping matches the latter the same way.
            String name = t.getClass().getSimpleName();

            if ("NodeMimetypeUnknownValidationException".equals(name))
            {
                return new Edu_SharingWebDAVServerException(rejectedStatus, MSERR_BAD_FILETYPE,
                        "The file type could not be determined and is therefore not permitted on this server.", e);
            }
            if ("NodeMimetypeValidationException".equals(name))
            {
                return new Edu_SharingWebDAVServerException(rejectedStatus, MSERR_BAD_FILETYPE,
                        "This file type is not permitted on this server.", e);
            }
            if ("NodeFileExtensionValidationException".equals(name))
            {
                return new Edu_SharingWebDAVServerException(rejectedStatus, MSERR_BAD_FILETYPE,
                        "The file extension does not match the detected file type.", e);
            }
            if ("NodeFileSizeExceededException".equals(name))
            {
                return new Edu_SharingWebDAVServerException(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        MSERR_FILE_SIZE, "The file exceeds the maximum permitted file size.", e);
            }
            if ("ContentQuotaException".equals(name))
            {
                return new Edu_SharingWebDAVServerException(SC_INSUFFICIENT_STORAGE, MSERR_OVER_QUOTA,
                        "The file was not saved because the storage quota is exhausted.", e);
            }
            // the virus scan exceptions are matched loosely on purpose, the implementing module is
            // external and its package is not part of the contract
            if (name.contains("VirusDetectedException"))
            {
                return new Edu_SharingWebDAVServerException(rejectedStatus, MSERR_VIRUS_INFECTED_UPLOAD,
                        "The file was rejected because the virus scan reported a finding.", e);
            }
            if (name.contains("VirusScanFailedException"))
            {
                return new Edu_SharingWebDAVServerException(rejectedStatus, MSERR_VIRUS_SCAN_FAILED,
                        "The file could not be checked for viruses and was therefore not saved.", e);
            }
        }
        return null;
    }
}
