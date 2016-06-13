package org.orcid.openid;

import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.service.UserInfoService;

/**
 * 
 * @author Will Simpson
 *
 */
public class OrcidOpenIdUserInfoService implements UserInfoService {

    @Override
    public UserInfo getByUsername(String username) {
        // XXX
        return createDummyUserInfo();
    }

    @Override
    public UserInfo getByUsernameAndClientId(String username, String clientId) {
        // XXX
        return createDummyUserInfo();
    }

    @Override
    public UserInfo getByEmailAddress(String email) {
        // XXX
        return createDummyUserInfo();
    }

    private UserInfo createDummyUserInfo() {
        DefaultUserInfo userInfo = new DefaultUserInfo();
        userInfo.setId(123L);
        userInfo.setName("Simpson");
        userInfo.setSub("abc");
        return userInfo;
    }

}
