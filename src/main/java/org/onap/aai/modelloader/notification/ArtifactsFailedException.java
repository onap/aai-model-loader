/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.modelloader.notification;

import java.io.Serial;
import java.util.List;
import org.onap.sdc.api.notification.IArtifactInfo;

/**
 * One or more artifacts of a distribution could not be downloaded or processed. Each of them has already had its
 * failure status published to SDC.
 */
class ArtifactsFailedException extends Exception {

    @Serial private static final long serialVersionUID = 1L;

    private final transient List<IArtifactInfo> reportedArtifacts;

    ArtifactsFailedException(String message, List<IArtifactInfo> reportedArtifacts, Throwable firstFailure) {
        super(message, firstFailure);
        this.reportedArtifacts = List.copyOf(reportedArtifacts);
    }

    List<IArtifactInfo> getReportedArtifacts() {
        return reportedArtifacts;
    }
}
