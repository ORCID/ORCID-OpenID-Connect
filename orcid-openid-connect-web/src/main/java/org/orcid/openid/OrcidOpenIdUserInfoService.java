package org.orcid.openid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.service.UserInfoService;

/**
 * 
 * @author Will Simpson
 *
 */
public class OrcidOpenIdUserInfoService implements UserInfoService {

    @Resource(name = "pooledDataSource")
    private DataSource dataSource;

    @Override
    public UserInfo getByUsername(String username) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement profileStatement = connection.prepareStatement("select credit_name from profile where orcid = ?");
            profileStatement.setString(1, username);
            ResultSet profileResults = profileStatement.executeQuery();
            profileResults.next();
            DefaultUserInfo userInfo = new DefaultUserInfo();
            userInfo.setId(UUID.randomUUID().getMostSignificantBits());
            userInfo.setName(profileResults.getString(1));
            userInfo.setSub(username);
            return userInfo;
        } catch (SQLException e) {
            throw new RuntimeException("Error looking up user info", e);
        }
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
