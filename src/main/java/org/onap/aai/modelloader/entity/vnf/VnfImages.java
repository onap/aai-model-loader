package org.onap.aai.modelloader.entity.vnf;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VnfImages {
  List<VnfImage> vnfImages;
}
