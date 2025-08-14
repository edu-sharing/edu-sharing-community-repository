package org.edu_sharing.service.share;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareInfoOplogData implements ShareInfoOplog {
    Long id;
    Long shareId;
    OpLogAction action;
    Date timestamp;
}
