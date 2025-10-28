package org.edu_sharing.restservices.collection.v1.model;

import lombok.Data;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.collection.CopyResult;

import java.util.ArrayList;

@Data
public class Copy {
    Node root;
    ArrayList<Entry> entries = new ArrayList<>();

    public record Entry(Node node, CopyResult.ErrorCode errorCode) {}
}
