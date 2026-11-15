package com.questclusters.compat.image;

/**
 * FTB Quests for 1.20.1 gives chapter images no id (1.21.1 does). Quest Clusters needs one to keep images in
 * clusters and editor data, so on 1.20.1 every image gets a persistent id (see ChapterImageIdMixin).
 */
public interface ImageId {
    long quest_clusters$id();

    void quest_clusters$setId(long id);
}
