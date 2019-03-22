/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.coyote;


/**
 * 本接口代替了Processor接口，成为所有Processor实现类的标准接口。其方法只有一个：public void action( ActionCode actionCode, Object param);
 * ActionCode是一个静态类，说白了是一堆常量（估计以后会改成enum），即对应不同的ActionCode，ActionHook要作出不同的动作，至于param是用于传递一些信息的，通常会把调用者“this”传递进去
 *
 * Action hook. Actions represent the callback mechanism used by
 * coyote servlet containers to request operations on the coyote connectors.
 * Some standard actions are defined in ActionCode, however custom
 * actions are permitted.
 * <p>
 * The param object can be used to pass and return informations related with the
 * action.
 * <p>
 * <p>
 * This interface is typically implemented by ProtocolHandlers, and the param
 * is usually a Request or Response object.
 *
 * @author Remy Maucherat
 */
public interface ActionHook {


    /**
     * Send an action to the connector.
     *
     * @param actionCode Type of the action
     * @param param      Action parameter
     */
    public void action(ActionCode actionCode, Object param);


}
