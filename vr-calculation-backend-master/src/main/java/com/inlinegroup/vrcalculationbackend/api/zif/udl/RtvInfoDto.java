package com.inlinegroup.vrcalculationbackend.api.zif.udl;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RtvInfoDto implements Comparable<RtvInfoDto> {
    private String uid;
    private String time;
    private String valueQualityId;
    private String valueStatusId;
    private String valueTypeId;
    private String value;
    private String annotation;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof RtvInfoDto tagData) {
            return this.uid.equals(tagData.uid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.uid.hashCode();
    }

    @Override
    public int compareTo(RtvInfoDto o) {
        return this.time.compareTo(o.time);
    }
}
