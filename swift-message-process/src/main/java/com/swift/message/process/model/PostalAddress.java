package com.swift.message.process.model;

import java.util.List;

/** ISO 20022 {@code PstlAdr} 地址信息。 */
public record PostalAddress(
        /** XML 路径：{@code PstlAdr/AdrTp/Cd}。含义：标准地址类型代码；非必传。 */
        String addressTypeCode,

        /** XML 路径：{@code PstlAdr/AdrTp/Prtry/Id}。含义：专有地址类型标识；可替代 {@code AdrTp/Cd}。 */
        String addressTypeProprietary,

        /** XML 字段：{@code Dept}。含义：部门；非必传。 */
        String department,

        /** XML 字段：{@code SubDept}。含义：子部门；非必传。 */
        String subDepartment,

        /** XML 字段：{@code StrtNm}。含义：街道名称；非必传。 */
        String streetName,

        /** XML 字段：{@code BldgNb}。含义：建筑物编号；非必传。 */
        String buildingNumber,

        /** XML 字段：{@code BldgNm}。含义：建筑物名称；非必传。 */
        String buildingName,

        /** XML 字段：{@code Flr}。含义：楼层；非必传。 */
        String floor,

        /** XML 字段：{@code PstBx}。含义：邮政信箱；非必传。 */
        String postBox,

        /** XML 字段：{@code Room}。含义：房间；非必传。 */
        String room,

        /** XML 字段：{@code PstCd}。含义：邮政编码；非必传。 */
        String postCode,

        /**
         * XML 字段：{@code TwnNm}。含义：城镇名称；ISO 20022 基础 XSD 中非必传。
         * CBPR+ 采用结构化或混合地址时通常为条件必填，具体以当前 Usage Guideline 为准。
         */
        String townName,

        /** XML 字段：{@code TwnLctnNm}。含义：城镇位置名称；非必传。 */
        String townLocationName,

        /** XML 字段：{@code DstrctNm}。含义：区县名称；非必传。 */
        String districtName,

        /** XML 字段：{@code CtrySubDvsn}。含义：国家行政区划；非必传。 */
        String countrySubDivision,

        /**
         * XML 字段：{@code Ctry}。含义：国家代码；ISO 20022 基础 XSD 中非必传。
         * CBPR+ 采用结构化或混合地址时通常为条件必填，具体以当前 Usage Guideline 为准。
         */
        String country,

        /**
         * XML 字段：{@code AdrLine}。含义：非结构化地址行；ISO 20022 基础 XSD 中可重复。
         * CBPR+ 对非结构化及混合地址的允许范围有版本和迁移期限制，不能仅依据基础 XSD 判断是否可用。
         */
        List<String> addressLines) {

    public PostalAddress {
        addressLines = addressLines == null ? List.of() : List.copyOf(addressLines);
    }
}
