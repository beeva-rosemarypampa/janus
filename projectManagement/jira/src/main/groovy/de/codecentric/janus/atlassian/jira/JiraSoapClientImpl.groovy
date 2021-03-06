/*
 * Copyright (C) 2012 codecentric AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.janus.atlassian.jira

import com.atlassian.jira.rpc.exception.RemoteValidationException
import com.atlassian.jira.rpc.soap.JiraSoapService

import java.rmi.RemoteException

import com.atlassian.jira.rpc.soap.beans.*
import de.codecentric.janus.atlassian.AtlassianException

/**
 * @author Ben Ripkens <bripkens.dev@gmail.com>
 */
class JiraSoapClientImpl implements JiraSoapClient {
    static final JIRA_GROUP_ROLE_ACTOR_IDENTIFIER = 'atlassian-group-role-actor'

    private final JiraSoapService service
    private final String token

    JiraSoapClientImpl(JiraSoapSession session) {
        service = session.getService()
        token = session.getAuthToken()
    }

    @Override
    RemoteGroup getGroup(String groupName) {
        return maskRemoteException {
            service.getGroup(token, groupName)
        }
    }

    @Override
    void deleteGroup(String groupName) {
        maskRemoteException(RemoteValidationException) {
            service.deleteGroup(token, groupName, null)
        }
    }

    @Override
    RemoteGroup createGroup(String groupName) {
        return maskRemoteException {
            // JavaDoc lists third argument as optional
            return service.createGroup(token, groupName, null)
        }
    }

    @Override
    RemoteProject getProject(String key) {
        return maskRemoteException {
            return service.getProjectByKey(token, key)
        }
    }

    @Override
    void deletePermissionScheme(String name) {
        maskRemoteException {
            service.deletePermissionScheme(token, name)
        }
    }

    @Override
    RemotePermissionScheme[] getPermissionSchemes() {
        return maskRemoteException {
            return service.getPermissionSchemes(token)
        }
    }

    @Override
    RemotePermissionScheme getPermissionScheme(String name) {
        return getPermissionSchemes().find { it.name == name }
    }

    @Override
    RemoteScheme[] getNotificationSchemes() {
        return maskRemoteException {
            return service.getNotificationSchemes(token)
        }
    }

    @Override
    RemoteScheme getNotificationScheme(String name) {
        return getNotificationSchemes().find { it.name == name }
    }

    @Override
    RemotePermissionScheme createPermissionScheme(String name) {
        return maskRemoteException {
            return service.createPermissionScheme(token, name, null)
        }
    }

    @Override
    RemotePermissionScheme addPermissionTo(RemotePermissionScheme scheme,
                                           RemotePermission permission,
                                           RemoteEntity entity) {
        return maskRemoteException {
            return service.addPermissionTo(token, scheme, permission, entity)
        }
    }

    @Override
    void deleteProject(String projectKey) {
        maskRemoteException(RemoteValidationException.class) {
            service.deleteProject(token, projectKey)
        }
    }

    @Override
    RemoteProject createProject(RemoteProject project) {
        return maskRemoteException {
            return service.createProjectFromObject(token, project)
        }
    }

    @Override
    RemoteProjectRole[] getProjectRoles() {
        return maskRemoteException {
            return service.getProjectRoles(token)
        }
    }

    @Override
    RemoteProjectRole getProjectRole(String name) {
        return getProjectRoles().find { it.name == name }
    }

    @Override
    RemoteUser createUser(String username, String password, String fullName,
                          String email) {
        return maskRemoteException {
            return service.createUser(token, username.toLowerCase(),
                    password, fullName, email)
        }
    }

    @Override
    void deleteUser(String username) {
        maskRemoteException(RemoteValidationException.class) {
            service.deleteUser(token, username)
        }
    }

    @Override
    void addUserToGroup(RemoteGroup group, RemoteUser user) {
        maskRemoteException {
            service.addUserToGroup(token, group, user)
        }
    }

    @Override
    void addGroupToRole(RemoteProject project, RemoteGroup group,
                        RemoteProjectRole role) {
        maskRemoteException {
            service.addActorsToProjectRole(token, [group.getName()] as String[],
                    role, project, JIRA_GROUP_ROLE_ACTOR_IDENTIFIER)
        }
    }

    @Override
    RemoteRoleActors getDefaultRoleActors(RemoteProjectRole role) {
        return maskRemoteException {
            return service.getDefaultRoleActors(token, role)
        }
    }

    @Override
    RemoteProjectRoleActors getProjectRoleActors(RemoteProject project,
                                                 RemoteProjectRole role) {
        return maskRemoteException {
            return service.getProjectRoleActors(token, role, project)
        }
    }

    private maskRemoteException(String ignore, Closure closure) {
        try {
            return closure()
        } catch (Exception ex) {
            // The remote exception are captured in a non-standard way.
            // The only possible way to identify the root cause is the toString
            // method. The root cause is not listed as ex.cause neither
            // part of ex.suppressed or ex.message.
            if (ignore != null && ex.toString().contains(ignore)) {
                return
            }

            if (ex instanceof RemoteException) {
                throw new AtlassianException(ex)
            }

            throw ex
        }
    }

    private maskRemoteException(Class<?> ignore, Closure closure) {
        return maskRemoteException(ignore.name, closure)
    }

    private maskRemoteException(Closure closure) {
        // the casting is necessary for the Java compiler to be able to
        // identify which overloaded method should be called.
        return maskRemoteException((String) null, closure)
    }
}
