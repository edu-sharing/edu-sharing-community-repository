package org.edu_sharing.service.handleservice;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.handle.api.HSAdapter;
import net.handle.api.HSAdapterFactory;
import net.handle.hdllib.*;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.ibatis.session.SqlSession;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleServiceImpl implements HandleService {


    public static final int DEFAULT_ADMIN_INDEX = 300;
    private final HandleConfig handleConfig;


    @PostConstruct
    @EventListener(RefreshScopeRefreshedEvent.class)
    public void loadConfig() {
        try {
            if (handleConfig.isEnabled()) {
                Objects.requireNonNull(handleConfig.getPrefix(), "repository.handleservice.prefix not set");
                Objects.requireNonNull(handleConfig.getRepoid(), "repository.handleservice.repoid not set");
                if (getMode() == HandleConfig.Mode.protocol) {
                    Objects.requireNonNull(handleConfig.getPrivkey(), "repository.handleservice.privkey not set");
                }
            }
            if (StringUtils.isNotBlank(handleConfig.getConfigDir())) {
                System.setProperty("net.handle.configDir", handleConfig.getConfigDir());
            } else {
                System.clearProperty("net.handle.configDir");
            }
        } catch (Throwable t) {
            log.error("Could not initialize handle service properly cause of error in config, please check config for \"repository.handleservice\"", t);
        }
    }

    @Override
    public boolean available() {
        try {
            handleServiceAvailable();
            return true;
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateId() throws Exception {
        return this.generateHandle();
    }

    @Override
    public String create(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        return createHandle(handleId, getDefautValues(getContentLink(properties)));
    }

    @Override
    public String update(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        return updateHandle(handleId, getDefautValues(getContentLink(properties)));
    }

    @Override
    public String delete(String handleId, String nodeId) throws Exception {
        //not implemented
        return "";
    }

    @Override
    public String getHandleIdProperty() {
        return CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID;
    }

    public String getId() {
        if (!handleConfig.isEnabled()) {
            return null;
        }
        return "0.NA/" + handleConfig.getPrefix();
    }

    /**
     * @return handle of the identity used for authentication, defaults to the prefix handle (0.NA/{prefix})
     */
    public String getAdminHandle() {
        if (StringUtils.isNotBlank(handleConfig.getAdminHandle())) {
            return handleConfig.getAdminHandle();
        }
        return getId();
    }


    public HandleValue[] getHandleValues(String handle, String[] values) {
        try {
            HSAdapter adapter = HSAdapterFactory.newInstance(getAdminHandle(), getAdminIndex(), getPrivateKeyBytes(), null);
            return adapter.resolveHandle(handle, values, null);

        } catch (HandleException | IOException e) {
            // TODO Auto-generated catch block
            log.error(e.getMessage(), e);
        }


        return null;
    }

    public void handleServiceAvailable() throws HandleException, IOException {
        HSAdapterFactory.newInstance(getAdminHandle(), getAdminIndex(), getPrivateKeyBytes(), null);
    }

    public String createHandle(String handle, final HandleValue[] values) throws Exception {
        HSAdapter adapter = HSAdapterFactory.newInstance(getAdminHandle(), getAdminIndex(), getPrivateKeyBytes(), null);
        adapter.createHandle(handle, values);
        return handle;
    }

    public String updateHandle(String handle, final HandleValue[] values) throws Exception {
        HSAdapter adapter = HSAdapterFactory.newInstance(getAdminHandle(), getAdminIndex(), getPrivateKeyBytes(), null);
        adapter.updateHandleValues(handle, values);
        return handle;
    }


    public void deleteHandle(String handle) throws Exception {
        HSAdapter adapter = HSAdapterFactory.newInstance(getAdminHandle(), getAdminIndex(), getPrivateKeyBytes(), null);
        adapter.deleteHandle(handle);
    }


    public AuthenticationInfo getAuthenticationInfo(String id, Integer idIndex) {
        PrivateKey privKey = loadPrivateKeyFromFile();
        if (privKey == null) {
            return null;
        }
        return new PublicKeyAuthenticationInfo(Util.encodeString(id), idIndex, privKey);
    }

    private PrivateKey loadPrivateKeyFromFile() {

        try {

            return getPrivateKey();

        } catch (Exception e) {
            log.error(e.getMessage(), e);

            return null;
        }
    }

    /**
     * loads the configured private key, either in handle format (e.g. admpriv.bin, unencrypted) or as PEM (PKCS#8 or PKCS#1)
     */
    PrivateKey getPrivateKey() throws Exception {
        byte[] bytes = readPrivateKeyFile();
        if (PemKeyReader.isPem(bytes)) {
            return PemKeyReader.readPrivateKey(bytes);
        }
        return Util.getPrivateKeyFromBytes(Util.decrypt(bytes, null), 0);
    }

    /**
     * @return private key in handle format as expected by {@link HSAdapterFactory}
     */
    byte[] getPrivateKeyBytes() throws IOException {
        byte[] bytes = readPrivateKeyFile();
        if (!PemKeyReader.isPem(bytes)) {
            return bytes;
        }
        try {
            return Util.encrypt(Util.getBytesFromPrivateKey(PemKeyReader.readPrivateKey(bytes)), null, Common.ENCRYPT_NONE);
        } catch (Exception e) {
            throw new IOException("could not convert pem private key " + handleConfig.getPrivkey() + " to handle format", e);
        }
    }

    private byte[] readPrivateKeyFile() throws IOException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        try (FileInputStream fin = new FileInputStream(handleConfig.getPrivkey())) {
            int r;
            while ((r = fin.read(buf)) >= 0)
                bout.write(buf, 0, r);
        }
        buf = bout.toByteArray();
        return buf;
    }


//    public static void main(String[] args) {
//
//        String handleId = RandomStringUtils.randomAlphabetic(HANDLE_ID_LENGTH);
//        System.out.println(handleId);
//        if (true) return;
//        HandleServiceImpl main;
//        try {
//            main = new HandleServiceImpl();
//
//            HandleValue[] hvs = main.getHandleValues(handleConfig.getPrefix() + "/901-LEO-ORG",
//                    new String[]{new String(Common.STD_TYPE_EMAIL), new String(Common.STD_TYPE_URL)});
//
//            for (HandleValue hv : hvs) {
//                System.out.println(hv.getDataAsString() + " " + hv.getIndex());
//            }
//
//            HandleValue hv = new HandleValue(1, Common.STD_TYPE_URL, "https://google.de".getBytes());
//
//            main.createHandle(handleConfig.getPrefix() + "/905-google", new HandleValue[]{hv});
//
//        } catch (HandleServiceNotConfiguredException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//    }

    @Override
    public boolean enabled() {
        return handleConfig.isEnabled();
    }

    /**
     * @return configured implementation mode, defaults to {@link HandleConfig.Mode#protocol}
     */
    public HandleConfig.Mode getMode() {
        return handleConfig.getMode() == null ? HandleConfig.Mode.protocol : handleConfig.getMode();
    }

    /**
     * @return index of the admin key at the prefix handle (0.NA/{prefix}), defaults to 300
     */
    public int getAdminIndex() {
        return handleConfig.getAdminIndex() == null ? DEFAULT_ADMIN_INDEX : handleConfig.getAdminIndex();
    }

    public String getEmail() {
        String mail = handleConfig.getEmail();
        return (mail == null) ? "info@edu-sharing.com" : mail;
    }

    public String generateHandle() throws SQLException {

        String id = HandleServiceImpl.generateUniqueHandleId();
        return handleConfig.getPrefix() + "/" + handleConfig.getRepoid() + id;
    }

    public HandleValue[] getDefautValues(String url) {
        HandleValue hvUrl = new HandleValue(1, Common.STD_TYPE_URL, url.getBytes());

        String mail = getEmail();

        HandleValue hvMail = new HandleValue(2, Common.STD_TYPE_EMAIL, mail.getBytes());
        HandleValue hvAdmin = getAdminValue(100, getId(), 200);

        if (!getId().equals(getAdminHandle())) {
            // the authenticating identity is not the prefix admin, so it needs its own admin value to modify the handle later on
            return new HandleValue[]{hvUrl, hvMail, hvAdmin, getAdminValue(101, getAdminHandle(), getAdminIndex())};
        }
        return new HandleValue[]{hvUrl, hvMail, hvAdmin};
    }

    private HandleValue getAdminValue(int index, String adminHandle, int adminIndex) {
        HandleValue hvAdmin = new HandleValue();
        hvAdmin.setIndex(index);
        hvAdmin.setType(Common.ADMIN_TYPE);
        hvAdmin.setData(Encoder.encodeAdminRecord(new AdminRecord(Util.encodeString(adminHandle), adminIndex,
                true, // addHandle
                true, // deleteHandle
                false, // addNA
                false, // deleteNA
                true, // readValue
                true, // modifyValue
                true, // removeValue
                true, // addValue
                true, // modifyAdmin
                true, // removeAdmin
                true, // addAdmin
                false  // listHandles
        )));
        return hvAdmin;
    }


    public static String HANDLE_TABLE = "EDU_HANDLE";

    public static String HANDLE_TABLE_ATT_ID = "ID";

    public static String HANDLE_QUERY_EXISTS = "select " + HANDLE_TABLE_ATT_ID + " from " + HANDLE_TABLE + " where lower(" + HANDLE_TABLE_ATT_ID + ") = lower(?)";

    public static String HANDLE_INSERT = "insert into " + HANDLE_TABLE + " (" + HANDLE_TABLE_ATT_ID + ") VALUES (?)";

    public static int HANDLE_ID_LENGTH = 8;

    public static synchronized String generateUniqueHandleId() throws SQLException {
        String handleId = RandomStringUtils.randomAlphabetic(HANDLE_ID_LENGTH);

        while (handleIdExists(handleId)) {
            handleId = RandomStringUtils.randomAlphabetic(HANDLE_ID_LENGTH);
        }

        //handle server does not differentiate upper/lowercase
        handleId = handleId.toUpperCase();

        insertHandleId(handleId);
        return handleId;
    }

    private static boolean handleIdExists(String id) throws SQLException {
        try (SqlSession session = new ConnectionDBAlfresco().getSqlSessionFactoryBean().openSession()) {
            PreparedStatement statement = session.getConnection().prepareStatement(HANDLE_QUERY_EXISTS);
            id = StringEscapeUtils.escapeSql(id);
            statement.setString(1, id);
            java.sql.ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static void insertHandleId(String id) throws SQLException {
        try (SqlSession session = new ConnectionDBAlfresco().getSqlSessionFactoryBean().openSession()) {

            PreparedStatement statement = session.getConnection().prepareStatement(HANDLE_INSERT);
            id = StringEscapeUtils.escapeSql(id);
            statement.setString(1, id);

            statement.executeUpdate();
            statement.getConnection().commit();
        }
    }

    @Override
    public String sync(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        // handleservice has no option to manage metadata
        return handleId;
    }
}



