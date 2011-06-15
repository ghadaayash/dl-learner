package org.dllearner.server;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.InvalidParameterException;


public class NKEGeizhals extends HttpServlet {
    private static Logger log = LoggerFactory.getLogger(NKEGeizhals.class);




    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        handle(httpServletRequest, httpServletResponse);
    }

    @Override
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        handle(httpServletRequest, httpServletResponse);
    }


    /**
     * *
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    private void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        Monitor mon = MonitorFactory.getTimeMonitor("NIFParameters.getInstance").start();
        String result = "";
        try {

            String action = "";
            if (isSet(httpServletRequest, "action")) {
                action = httpServletRequest.getParameter("action");
                //use the function one of
                if (!oneOf(action, "learn", "xml")) {
                    throw new InvalidParameterException("Wrong parameter value for \"action\", must be one of ( learn, xml ) " + getDocumentation(httpServletRequest.getRequestURL().toString()));
                }
            } else {
                throw new InvalidParameterException("No parameter 'conf' found. " + getDocumentation(httpServletRequest.getRequestURL().toString()));
            }

            if (action.equals("learn")) {
                String json = "";
                if (isSet(httpServletRequest, "data")) {
                    json = httpServletRequest.getParameter("data");

                    //Object obj=JSONValue.parse(s);
                    // JSONArray array=(JSONArray)obj;

                    /*Iterator i = obj.entrySet().iterator();
                   while (i.hasNext()) {
                      Map.Entry e = (Map.Entry)i.next();
                      System.out.println("Key: " + e.getKey());
                      System.out.println("Value: " + e.getValue());
                   } */

                } else {
                    throw new InvalidParameterException("No parameter 'data' found. " + getDocumentation(httpServletRequest.getRequestURL().toString()));
                }
            }

            //check parameters
            /*String conf = "";
            if (isSet(httpServletRequest, "conf")) {
                conf = httpServletRequest.getParameter("conf");
            } else {
                throw new InvalidParameterException("No parameter 'conf' found. " + getDocumentation(httpServletRequest.getRequestURL().toString()));
            }*/

            PrintWriter pw = httpServletResponse.getWriter();
            log.debug("Request handled: " + logMonitor(mon.stop()));
            pw.print(result);
            pw.close();

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() + printParameterMap(httpServletRequest);
            log.error(msg);
            httpServletResponse.setContentType("text/plain");
            PrintWriter out = httpServletResponse.getWriter();
            out.println(msg);
            out.close();

        } catch (Exception e) {
            String msg = "An error occured: " + e.getMessage() + printParameterMap(httpServletRequest);
            log.error(msg, e);
            httpServletResponse.setContentType("text/plain");
            PrintWriter out = httpServletResponse.getWriter();
            out.println(msg);
            out.close();

        }

    }

    /**
     * Examples are from NIF
     *
     * @param serviceUrl
     * @return
     */
    public static String getDocumentation(String serviceUrl) {
        String doc = "";
        try {
            doc = "\nExample1: \n " + serviceUrl + "?input=" + URLEncoder.encode("That's a lot of nuts! That'll be four bucks, baby! You want fries with that? ", "UTF-8") + "&type=text";
            doc += "\nExample2: \n " + serviceUrl + "?input=" + URLEncoder.encode("That's a lot of nuts! That's a lot of nuts! ", "UTF-8") + "&type=text";
        } catch (Exception e) {
            log.error("", e);
        }
        return doc;
    }

    public static boolean isSet(HttpServletRequest httpServletRequest, String name) {
        log.trace("isSet(" + name + ")");
        log.trace(httpServletRequest.getParameterValues(name) + "");
        return httpServletRequest.getParameterValues(name) != null && httpServletRequest.getParameterValues(name).length == 1 && httpServletRequest.getParameter(name).length() > 0;
    }

    public static boolean oneOf(String value, String... possibleValues) {
        for (String s : possibleValues) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    protected static String logMonitor(Monitor m) {
        return "needed: " + m.getLastValue() + " ms. (" + m.getTotal() + " total)";
    }

    public static String printParameterMap(HttpServletRequest httpServletRequest) {
        StringBuffer buf = new StringBuffer();
        for (Object key : httpServletRequest.getParameterMap().keySet()) {
            buf.append("\nParameter: " + key + " Values: ");
            for (String s : httpServletRequest.getParameterValues((String) key)) {
                buf.append(((s.length() > 200) ? s.substring(0, 200) + "..." : s) + " ");
            }
        }
        return buf.toString();
    }

}
