package org.edu_sharing.service.tracking.ibatis;

import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface EduTrackingMapper {
    List<NodeResult> eduAlteredNodes(@Param("from") Date from);
    List<NodeData> eduNodeData(@Param("id") String id, @Param("format") String format, @Param("from") Date from);

}
