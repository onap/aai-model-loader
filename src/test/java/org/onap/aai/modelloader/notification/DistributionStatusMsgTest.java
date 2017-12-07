/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.modelloader.notification;

import org.junit.Assert;
import org.junit.Test;
import org.openecomp.sdc.utils.DistributionStatusEnum;

public class DistributionStatusMsgTest {

    @Test
    public void testEntireClass(){
        DistributionStatusMsg statusMsg = new DistributionStatusMsg(DistributionStatusEnum.DEPLOY_OK, "id-1", "consumer-1", "http://dummyurl");
        Assert.assertEquals(statusMsg.getStatus(), DistributionStatusEnum.DEPLOY_OK);
        Assert.assertEquals(statusMsg.getDistributionID(), "id-1");
        Assert.assertEquals(statusMsg.getConsumerID(), "consumer-1");
        Assert.assertEquals(statusMsg.getArtifactURL(), "http://dummyurl");
        Assert.assertNotNull(statusMsg.getTimestamp());
    }
}
