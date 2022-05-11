/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class ISO8601DateFormat {

    

    public static String format(Date isoDate) {

        Calendar calendar = new GregorianCalendar();

        calendar.setTime(isoDate);

        StringBuffer formatted = new StringBuffer();

        padInt(formatted, calendar.get(Calendar.YEAR), 4);

        formatted.append('-');

        padInt(formatted, calendar.get(Calendar.MONTH) + 1, 2);

        formatted.append('-');

        padInt(formatted, calendar.get(Calendar.DAY_OF_MONTH), 2);

        formatted.append('T');

        padInt(formatted, calendar.get(Calendar.HOUR_OF_DAY), 2);

        formatted.append(':');

        padInt(formatted, calendar.get(Calendar.MINUTE), 2);

        formatted.append(':');

        padInt(formatted, calendar.get(Calendar.SECOND), 2);

        return formatted.toString();

    }

    

    public static Date parse(String isoDate) {

        Date parsed = null;

        try {

            int offset = 0;

            int year = Integer.parseInt(isoDate.substring(offset, offset += 4));

            if (isoDate.charAt(offset) != '-') {

                throw new IndexOutOfBoundsException("Expected - character but found " + isoDate.charAt(offset));

            }

            int month = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));

            if (isoDate.charAt(offset) != '-') {

                throw new IndexOutOfBoundsException("Expected - character but found " + isoDate.charAt(offset));

            }

            int day = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));

            if (isoDate.charAt(offset) != 'T') {

                throw new IndexOutOfBoundsException("Expected T character but found " + isoDate.charAt(offset));

            }

            int hour = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));

            if (isoDate.charAt(offset) != ':') {

                throw new IndexOutOfBoundsException("Expected : character but found " + isoDate.charAt(offset));

            }

            int minutes = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));

            if (isoDate.charAt(offset) != ':') {

                throw new IndexOutOfBoundsException("Expected : character but found " + isoDate.charAt(offset));

            }

            int seconds = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));

            if (isoDate.charAt(offset) != '.') {

                throw new IndexOutOfBoundsException("Expected . character but found " + isoDate.charAt(offset));

            }

            int milliseconds = Integer.parseInt(isoDate.substring(offset += 1, offset += 3));

            String timezoneId;

            char timezoneIndicator = isoDate.charAt(offset);

            if (timezoneIndicator == '+' || timezoneIndicator == '-') {

                timezoneId = "GMT" + isoDate.substring(offset);

            } else  if (timezoneIndicator == 'Z') {

                timezoneId = "GMT";

            } else  {

                throw new IndexOutOfBoundsException("Invalid time zone indicator " + timezoneIndicator);

            }

            TimeZone timezone = TimeZone.getTimeZone(timezoneId);

            if (!timezone.getID().equals(timezoneId)) {

                throw new IndexOutOfBoundsException();

            }

            Calendar calendar = new GregorianCalendar(timezone);

            calendar.setLenient(false);

            calendar.set(Calendar.YEAR, year);

            calendar.set(Calendar.MONTH, month - 1);

            calendar.set(Calendar.DAY_OF_MONTH, day);

            calendar.set(Calendar.HOUR_OF_DAY, hour);

            calendar.set(Calendar.MINUTE, minutes);

            calendar.set(Calendar.SECOND, seconds);

            calendar.set(Calendar.MILLISECOND, milliseconds);

            parsed = calendar.getTime();

        } catch (IndexOutOfBoundsException e) {

            System.out.println("Failed to parse date " + isoDate);

        } catch (NumberFormatException e) {
        	System.out.println("Failed to parse date " + isoDate);

        } catch (IllegalArgumentException e) {
        	System.out.println("Failed to parse date " + isoDate);

        }

        return parsed;
    }

    private static void padInt(StringBuffer buffer, int value, int length) {

        String strValue = Integer.toString(value);

        for (int i = length - strValue.length(); i > 0; i--) {
            buffer.append('0');
        }
        buffer.append(strValue);

    }

}
