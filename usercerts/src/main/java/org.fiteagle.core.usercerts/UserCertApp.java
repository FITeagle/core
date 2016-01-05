package org.fiteagle.core.usercerts;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dne on 21.11.15.
 */
@ApplicationPath("/")
public class UserCertApp extends Application {


    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classSet = new HashSet<Class<?>>();
        classSet.add(UserCertService.class);
        return classSet;
    }
}
