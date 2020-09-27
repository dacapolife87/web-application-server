package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import http.HttpRequest;
import http.HttpResponse;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            HttpRequest request = new HttpRequest(in);
            HttpResponse response = new HttpResponse(out);

            String path = getDefaultPath(request.getPath());
            log.debug("Content-Length : {}",request.getHeader("Content-Length"));

            if("/user/create".equals(path)){
                User user = new User(request.getParameter("userId"),request.getParameter("password"),request.getParameter("name"),request.getParameter("email"));
                log.debug("User : {}",user);
                DataBase.addUser(user);

                response.sendRedirect("/index.html");
            } else if("/user/login".equals(path)) {
                log.debug("userId : {}, password : {}",request.getParameter("userId"),request.getParameter("password"));
                User user = DataBase.findUserById(request.getParameter("userId"));
                if(user == null){
                    log.debug("User Not Found!");
                    response.sendRedirect("/login_failed.html");
                }else if(user.getPassword().equals(request.getParameter("password"))) {
                    log.debug("login success!!");
                    response.addHeader("Set-Cookie", "logined=true");
                    response.sendRedirect("/index.html");
                }else{
                    log.debug("Password Mismatch!");
                    response.sendRedirect("/login_failed.html");
                }
            } else if("/user/list".equals(path)){
                if(isLogin(request.getHeader("Cookie"))){
                    Collection<User> users = DataBase.findAll();
                    StringBuilder sb = new StringBuilder();
                    sb.append("<table border='1'>");
                    for(User user : users){
                        sb.append("<tr>");
                        sb.append("<td>"+user.getUserId() + "</td>");
                        sb.append("<td>"+user.getName() + "</td>");
                        sb.append("<td>"+user.getEmail() + "</td>");
                        sb.append("</tr>");
                    }
                    sb.append("</table>");
                    response.forwardBody(sb.toString());
                }else{
                    log.debug("NotLogined!!");
                    response.forward("/user/login.html");
                    return;
                }

            }else {
                response.forward(path);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private boolean isLogin(String cookieValue){
        Map<String, String> cookies = HttpRequestUtils.parseCookies(cookieValue);
        String value = cookies.get("logined");
        if(value == null){
            return false;
        }

        return Boolean.parseBoolean(value);
    }
    private String getDefaultPath(String path) {
        if(path.equals("/")) {
            return "/index.html";
        }
        return path;
    }
}
