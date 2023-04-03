package com.example.onepic.PictureModule

class JpegConstant {
    var  nameHashMap: HashMap<Int?, String?> = object : HashMap<Int?, String?>() {
        init {
            put(480, "APP1")
            put(481, "APP2")
            put(482, "APP3")
            put(483, "APP4")
            put(492, "APP13")
            put(493, "APP14")
            put(494, "APP15")
            put(479, "JFIF")
            put(447, "SOF0")
            put(448, "SOF1")
            put(449, "SOF2")
            put(450, "SOF3")
            put(451, "DHT")
            put(452, "SOF5")
            put(453, "SOF6")
            put(454, "SOF7")
            put(455, "SOF8")
            put(456, "SOF9")
            put(457, "SOF10")
            put(458, "SOF11")
            put(459, "DAC")
            put(460, "SOF13")
            put(461, "SOF14")
            put(462, "SOF15")
            put(476, "DRI")
            put(463, "RST0")
            put(464, "RST1")
            put(465, "RST2")
            put(466, "RST3")
            put(467, "RST4")
            put(468, "RST5")
            put(469, "RST6")
            put(470, "RST7")
            put(471, "SOI")
            put(472, "EOI")
            put(473, "SOS")
            put(474, "DQT")
            put(475, "DNL")
            put(509, "COM")
            put(503, "SOM")
            put(496, "MEDIA1")
            put(497, "MEDIA2")
            put(504, "EOM")
        }
    }
    val JPEG_SOM_MARKER = 0xff + 0xf8
    val JPEG_MEDIA1_MARKER = 0xff + 0xf1
    val JPEG_MEDIA2_MARKER = 0xff + 0xf2
    val JPEG_EOM_MARKER = 0xff + 0xf9

    val JPEG_APP1_MARKER = 0xff + 0xe1
    val JPEG_APP2_MARKER = 0xff + 0xe2
    val JPEG_APP3_MARKER = 0xff + 0xe3
    val JPEG_APP4_MARKER = 0xff + 0xe4
    val JPEG_APP13_MARKER = 0xff + 0xed
    val JPEG_APP14_MARKER = 0xff + 0xee
    val JPEG_APP15_MARKER = 0xff + 0xef

    val JFIF_MARKER = 0xff + 0xe0
    val SOF0_MARKER = 0xff + 0xc0
    val SOF1_MARKER = 0xff + 0xc1
    val SOF2_MARKER = 0xff + 0xc2
    val SOF3_MARKER = 0xff + 0xc3
    val DHT_MARKER = 0xff + 0xc4
    val SOF5_MARKER = 0xff + 0xc5
    val SOF6_MARKER = 0xff + 0xc6
    val SOF7_MARKER = 0xff + 0xc7
    val SOF8_MARKER = 0xff + 0xc8
    val SOF9_MARKER = 0xff + 0xc9
    val SOF10_MARKER = 0xff + 0xca
    val SOF11_MARKER = 0xff + 0xcb
    val DAC_MARKER = 0xff + 0xcc
    val SOF13_MARKER = 0xff + 0xcd
    val SOF14_MARKER = 0xff + 0xce
    val SOF15_MARKER = 0xff + 0xcf

    // marker for restart intervals
    val DRI_MARKER = 0xff + 0xdd
    val RST0_MARKER = 0xff + 0xd0
    val RST1_MARKER = 0xff + 0xd1
    val RST2_MARKER = 0xff + 0xd2
    val RST3_MARKER = 0xff + 0xd3
    val RST4_MARKER = 0xff + 0xd4
    val RST5_MARKER = 0xff + 0xd5
    val RST6_MARKER = 0xff + 0xd6
    val RST7_MARKER = 0xff + 0xd7

    val SOI_MARKER = 0xff + 0xd8
    val EOI_MARKER = 0xff + 0xd9
    val SOS_MARKER = 0xff + 0xda
    val DQT_MARKER = 0xff + 0xdb
    val DNL_MARKER = 0xff + 0xdc
    val COM_MARKER = 0xff + 0xfe

    val MARKERS = intArrayOf(
        JFIF_MARKER,
        JPEG_APP1_MARKER,
        JPEG_APP2_MARKER,
        JPEG_APP3_MARKER,
        JPEG_APP4_MARKER,
        JPEG_APP13_MARKER,
        JPEG_APP14_MARKER,
        JPEG_APP15_MARKER,
        COM_MARKER,
        SOF0_MARKER,
        SOF1_MARKER,
        SOF2_MARKER,
        SOF3_MARKER,
        DHT_MARKER,
        SOF5_MARKER,
        SOF6_MARKER,
        SOF7_MARKER,
        SOF8_MARKER,
        SOF9_MARKER,
        SOF10_MARKER,
        SOF11_MARKER,
        DAC_MARKER,
        SOF13_MARKER,
        SOF14_MARKER,
        SOF15_MARKER,  // marker for restart intervals
        DRI_MARKER,
        RST0_MARKER,
        RST1_MARKER,
        RST2_MARKER,
        RST3_MARKER,
        RST4_MARKER,
        RST5_MARKER,
        RST6_MARKER,
        RST7_MARKER,
        EOI_MARKER,
        SOS_MARKER,
        DQT_MARKER,
        DNL_MARKER,
        COM_MARKER,
        SOI_MARKER,
        JPEG_SOM_MARKER

    )
}