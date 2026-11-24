package com.ptit.cvrecruiter.web;

/** Một nhóm API liên quan. Mỗi controller tự khai báo các tuyến của mình với {@link Router}. */
public interface Controller {

    void register(Router router);
}
