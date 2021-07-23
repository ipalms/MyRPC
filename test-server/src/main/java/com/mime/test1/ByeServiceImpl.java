package com.mime.test1;

import com.mime.rpc.annotation.Service;
import com.mime.rpc.api.ByeService;


@Service
public class ByeServiceImpl implements ByeService {

    @Override
    public String bye(String name) {
        return "8686  bye, " + name;
    }
}
