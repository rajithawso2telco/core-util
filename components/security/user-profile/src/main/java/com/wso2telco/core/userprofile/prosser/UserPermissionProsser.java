/*******************************************************************************
 * Copyright  (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) All Rights Reserved.
 * <p>
 * WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.core.userprofile.prosser;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.hostobjects.internal.HostObjectComponent;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.user.mgt.stub.types.carbon.UIPermissionNode;
import org.wso2.carbon.utils.CarbonUtils;

import com.wso2telco.core.userprofile.dto.UserPermissionDTO;
import com.wso2telco.core.userprofile.permission.impl.UserRolePermission;
import com.wso2telco.core.userprofile.permission.impl.UserRolePermissionFactory;
import com.wso2telco.core.userprofile.util.AdminServicePath;
import com.wso2telco.core.userprofile.util.UserRolePermissionType;

public class UserPermissionProsser {

	private final Log log = LogFactory.getLog(UserPermissionProsser.class);

	UserRolePermissionFactory userRolePermissionFactory;

	public UserPermissionProsser() {

		userRolePermissionFactory = new UserRolePermissionFactory();
	}

	public List<String> getUserPermissionsByUserName(String userName, UserRolePermissionType userRolePermissionType) {

		List<String> userRolePermissionList = null;

		try {

			APIManagerConfiguration config = HostObjectComponent.getAPIManagerConfiguration();
			String userAdminServiceEndpoint = config.getFirstProperty(APIConstants.AUTH_MANAGER_URL)
					+ AdminServicePath.USER_ADMIN.getTObject();
			String adminUsername = config.getFirstProperty(APIConstants.AUTH_MANAGER_USERNAME);
			String adminPassword = config.getFirstProperty(APIConstants.AUTH_MANAGER_PASSWORD);

			UserAdminStub userAdminStub = new UserAdminStub(userAdminServiceEndpoint);
			CarbonUtils.setBasicAccessSecurityHeaders(adminUsername, adminPassword, userAdminStub._getServiceClient());

			UserRoleProsser userRoleRetriever = new UserRoleProsser();
			List<String> currentUserRoleList = userRoleRetriever.getRolesByUserName(userName);

			for (Iterator<String> iterator = currentUserRoleList.iterator(); iterator.hasNext();) {

				String roleName = iterator.next();

				UIPermissionNode rolePermissions = userAdminStub.getRolePermissions(roleName);

				UserRolePermission userRolePermission = userRolePermissionFactory
						.getUserRolePermissionExecuter(userRolePermissionType);
				userRolePermissionList = userRolePermission.getUserRolePermissions(rolePermissions);

				if (!userRolePermissionList.isEmpty()) {

					break;
				}
			}
		} catch (RemoteException | UserAdminUserAdminException e) {

			log.error("unable to retrieve " + userRolePermissionType + " permissions for user " + userName + " : ", e);
		}

		if (userRolePermissionList != null && !userRolePermissionList.isEmpty()) {

			return userRolePermissionList;
		} else {

			return Collections.emptyList();
		}
	}

	public UserPermissionDTO getUserPermissions(String userName, UserRolePermissionType userRolePermissionType) {

		UserPermissionDTO userPermissionDTO = null;

		List<String> userRolePermissionList = getUserPermissionsByUserName(userName, userRolePermissionType);

		if (!userRolePermissionList.isEmpty()) {

			String[] userPermissions = new String[userRolePermissionList.size()];
			userPermissions = userRolePermissionList.toArray(userPermissions);
			userPermissionDTO = new UserPermissionDTO();

			userPermissionDTO = fillUserPermissionDTO(userPermissions, userPermissionDTO);
		}

		return userPermissionDTO;
	}

	private UserPermissionDTO fillUserPermissionDTO(String[] userPermissions, UserPermissionDTO userPermissionDTO) {
		
		userPermissionDTO.setUserPermissions(userPermissions);
		
		return userPermissionDTO;
	}
}
