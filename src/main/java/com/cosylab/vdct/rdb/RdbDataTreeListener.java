package com.cosylab.vdct.rdb;

public interface RdbDataTreeListener {

	// Called whenever an ioc has been selected

    /**
     *
     * @param iocId iocId
     */
    public void iocSelected (String iocId);
    
    // Called whenever a group id has been selected

    /**
     *
     * @param iocId iocId
     * @param groupId groupId
     */
    public void groupSelected (String iocId, String groupId);

    // Called whenever a version of a group has been selected

    /**
     *
     * @param iocId iocId
     * @param groupId groupId
     * @param version version
     */
    public void versionSelected (String iocId, String groupId, String version);
};
