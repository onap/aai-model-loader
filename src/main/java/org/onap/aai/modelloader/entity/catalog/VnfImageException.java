/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.aai.modelloader.entity.catalog;

import java.util.Optional;

/**
 * Exception class used by the VnfCatalogArtifactHandler
 */
class VnfImageException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String imageId;
    private final transient Optional<Integer> resultCode;

    public VnfImageException(String imageId) {
        this.imageId = imageId;
        this.resultCode = Optional.empty();
    }

    public VnfImageException(String imageId, int resultCode) {
        this.imageId = imageId;
        this.resultCode = Optional.of(resultCode);
    }

    public VnfImageException(Exception e) {
        this.imageId = e.getMessage();
        this.resultCode = Optional.empty();
    }

    public String getImageId() {
        return imageId;
    }

    public Optional<Integer> getResultCode() {
        return resultCode;
    }

}