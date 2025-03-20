package com.inlinegroup.vrcalculationbackend.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VRTypeCalculation implements Comparable<VRTypeCalculation> {
    private String name;
    private String zifUid;
    private String activeVrType;

    @Override
    public int compareTo(VRTypeCalculation o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof VRTypeCalculation vrTypeCalculation) {
            return this.zifUid.equals(vrTypeCalculation.zifUid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.zifUid.hashCode();
    }
}