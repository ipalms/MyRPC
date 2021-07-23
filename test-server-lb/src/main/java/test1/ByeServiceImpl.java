package test1;

import com.mime.rpc.annotation.Service;
import com.mime.rpc.api.ByeService;


@Service
public class ByeServiceImpl implements ByeService {

    @Override
    public String bye(String name) {
        return "8787 bye, " + name;
    }
}
