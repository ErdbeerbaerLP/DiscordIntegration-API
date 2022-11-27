package de.erdbeerbaerlp.dcintegrationapi;

public class Link {
    private String uuid = "";
    private String dcID = "";

    public Link(String uuid, String dcID){
       this.uuid = uuid;
       this.dcID = dcID;
    }
    public String getDcID() {
        if(dcID == null) return "";
        return dcID;
    }
    public String getUuid() {
        if(uuid == null) return "";
        return uuid;
    }
}
