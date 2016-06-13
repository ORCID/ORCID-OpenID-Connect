/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.openid;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.orcid.core.manager.OrcidSecurityManager;
import org.orcid.core.oauth.OrcidProfileUserDetails;
import org.orcid.core.security.DeprecatedProfileException;
import org.orcid.core.security.UnclaimedProfileExistsException;
import org.orcid.persistence.dao.EmailDao;
import org.orcid.persistence.dao.ProfileDao;
import org.orcid.persistence.jpa.entities.EmailEntity;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.orcid.utils.OrcidStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Declan Newman (declan) Date: 13/02/2012
 */
public class OrcidOpenIdUserDetailsService implements UserDetailsService {

    @Resource
    private ProfileDao profileDao;

    @Resource
    private EmailDao emailDao;

    @Resource
    private OrcidSecurityManager securityMgr;

    @Resource(name = "pooledDataSource")
    private DataSource dataSource;

    @Value("${org.orcid.core.baseUri}")
    private String baseUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidOpenIdUserDetailsService.class);

    /**
     * Locates the user based on the username. In the actual implementation, the
     * search may possibly be case insensitive, or case insensitive depending on
     * how the implementation instance is configured. In this case, the
     * <code>UserDetails</code> object that comes back may have a username that
     * is of a different case than what was actually requested..
     * 
     * @param username
     *            the username identifying the user whose data is required.
     * @return a fully populated user record (never <code>null</code>)
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException
     *             if the user could not be found or the user has no
     *             GrantedAuthority
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LOGGER.info("About to load user by username = {}", username);
        ProfileEntity profile = obtainEntity(username);

        if (profile == null) {
            throw new UsernameNotFoundException("Bad username or password");
        }
        if (profile.getPrimaryRecord() != null) {
            throw new DeprecatedProfileException("orcid.frontend.security.deprecated_with_primary", profile.getPrimaryRecord().getId(), profile.getId());
        }
//        if (!profile.getClaimed() && !securityMgr.isAdmin()) {
//            throw new UnclaimedProfileExistsException("orcid.frontend.security.unclaimed_exists");
//        }
        if (profile.getDeactivationDate() != null && !securityMgr.isAdmin()) {
            throw new DisabledException("Account not active, please call helpdesk");
        }

        String primaryEmail = null;

        // Clients doesnt have primary email, so, we need to cover that case.
        if (profile.getPrimaryEmail() != null)
            primaryEmail = profile.getPrimaryEmail().getId();

        OrcidProfileUserDetails userDetails = null;

        if (profile.getOrcidType() != null) {
            userDetails = new OrcidProfileUserDetails(profile.getId(), primaryEmail, profile.getEncryptedPassword(), profile.getOrcidType(), profile.getGroupType());
        } else {
            userDetails = new OrcidProfileUserDetails(profile.getId(), primaryEmail, profile.getEncryptedPassword());
        }

        return userDetails;
    }

    private ProfileEntity obtainEntity(String username) {
        ProfileEntity profile = null;
        if (!StringUtils.isEmpty(username)) {
            if (OrcidStringUtils.isValidOrcid(username)) {
                profile = profileDao.find(username);
            } else {
                try (Connection connection = dataSource.getConnection()) {
                    PreparedStatement statement = connection.prepareStatement("select * from email where email = ?");
                    statement.setString(1, username);
                    ResultSet results = statement.executeQuery();
                    results.next();
                    String orcid = results.getString(4);
                    if (orcid != null) {
                        PreparedStatement profileStatement = connection.prepareStatement("select * from profile where orcid = ?");
                        profileStatement.setString(1, orcid);
                        ResultSet profileResults = profileStatement.executeQuery();
                        profileResults.next();
                        String encryptedPassword = profileResults.getString(12);
                        ProfileEntity profileEntity = new ProfileEntity(orcid);
                        profileEntity.setEncryptedPassword(encryptedPassword);
                        return profileEntity;
                        
                    }

                } catch (SQLException e) {
                    throw new RuntimeException("Error looking up email", e);
                }
                EmailEntity emailEntity = emailDao.findCaseInsensitive(username);
                if (emailEntity != null) {
                    profile = emailEntity.getProfile();
                }
            }
        }
        return profile;
    }
}
