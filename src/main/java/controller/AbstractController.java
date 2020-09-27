package controller;

import http.HttpRequest;
import http.HttpResponse;

public abstract class AbstractController implements Controller {
    @Override
    public void service(HttpRequest request, HttpResponse response) {

    }

    abstract void doGet(HttpRequest request, HttpResponse response);
    abstract void doPost(HttpRequest request, HttpResponse response);
}
