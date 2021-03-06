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

package de.codecentric.janus.atlassian.confluence

import com.atlassian.confluence.rpc.soap.ConfluenceSoapService
import com.atlassian.confluence.rpc.soap.beans.RemoteSpace
import java.rmi.RemoteException
import de.codecentric.janus.atlassian.AtlassianException
import com.atlassian.confluence.rpc.soap.beans.RemoteUser

/**
 * Refer to
 * https://developer.atlassian.com/display/CONFDEV/Confluence+XML-RPC+and+SOAP+APIs
 * for more information.
 *
 * @author Ben Ripkens <bripkens.dev@gmail.com>
 */
class ConfluenceSoapClientImpl implements ConfluenceSoapClient {
    private final ConfluenceSoapService service
    private final String token

    ConfluenceSoapClientImpl(ConfluenceSoapSession session) {
        service = session.getService()
        token = session.getAuthToken()
    }

    @Override
    RemoteSpace addSpace(RemoteSpace space) {
        return maskRemoteException {
            return service.addSpace(token, space)
        }
    }

    @Override
    void deleteSpace(String key) {
        maskRemoteException('No space found for space key') {
            service.removeSpace(token, key)
        }
    }

    @Override
    RemoteSpace getSpace(String key) {
        return maskRemoteException {
            return service.getSpace(token, key)
        }
    }

    @Override
    void createUser(RemoteUser user, String password) {
        maskRemoteException {
            service.addUser(token, user, password)
        }
    }

    @Override
    RemoteUser getUser(String username) {
        return maskRemoteException {
            return service.getUser(token, username)
        }
    }

    @Override
    void deleteUser(String username) {
        maskRemoteException('No user with username') {
            service.removeUser(token, username)
        }
    }

    @Override
    void removeAnonymousPermissionFromSpace(SpacePermission permission,
                                            String spaceKey) {
        maskRemoteException {
            service.removeAnonymousPermissionFromSpace(token,
                    permission.key,
                    spaceKey)
        }
    }

    @Override
    void removeAllAnonymousPermissionsFromSpace(String spaceKey) {
        for (SpacePermission permission : SpacePermission.values()) {
            removeAnonymousPermissionFromSpace(permission, spaceKey)
        }
    }

    @Override
    Collection<String> getPermissions(String spaceKey) {
        return maskRemoteException {
            return service.getPermissions(token, spaceKey)
        }
    }

    @Override
    void addPermissionToSpace(SpacePermission permission, String entityName,
                              String spaceKey) {
        maskRemoteException {
            service.addPermissionToSpace(token, permission.key, entityName,
                    spaceKey)
        }
    }

    @Override
    void addGroup(String name) {
        maskRemoteException {
            service.addGroup(token, name)
        }
    }

    @Override
    void deleteGroup(String groupName) {
        maskRemoteException('does not exist') {
            service.removeGroup(token, groupName, null)
        }
    }

    @Override
    boolean hasGroup(String groupName) {
        return maskRemoteException {
            return service.hasGroup(token, groupName)
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
