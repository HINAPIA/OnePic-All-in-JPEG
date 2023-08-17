export default class JpegConstant {
    constructor() {
        this.nameHashMap = new Map([
            [480, "APP1"],
            [481, "APP2"],
            [482, "APP3"],
            [483, "APP4"],
            [492, "APP13"],
            [493, "APP14"],
            [494, "APP15"],
            [479, "JFIF"],
            [447, "SOF0"],
            [448, "SOF1"],
            [449, "SOF2"],
            [450, "SOF3"],
            [451, "DHT"],
            [452, "SOF5"],
            [453, "SOF6"],
            [454, "SOF7"],
            [455, "SOF8"],
            [456, "SOF9"],
            [457, "SOF10"],
            [458, "SOF11"],
            [459, "DAC"],
            [460, "SOF13"],
            [461, "SOF14"],
            [462, "SOF15"],
            [476, "DRI"],
            [463, "RST0"],
            [464, "RST1"],
            [465, "RST2"],
            [466, "RST3"],
            [467, "RST4"],
            [468, "RST5"],
            [469, "RST6"],
            [470, "RST7"],
            [471, "SOI"],
            [472, "EOI"],
            [473, "SOS"],
            [474, "DQT"],
            [475, "DNL"],
            [509, "COM"],
            [503, "SOM"],
            [496, "MEDIA1"],
            [497, "MEDIA2"],
            [504, "EOM"],
            [265, "XOI"],
            [275, "XOT"],
            [285, "XOA"],
        ]);
        this.JPEG_SOM_MARKER = 0xff + 0xf8;
        this.XOI = 0xff + 0x10;
        this.XOA = 0xff + 0x30;
        this.XOT = 0xff + 0x20;
        this.JPEG_APP1_MARKER = 0xff + 0xe1;
        this.JPEG_APP2_MARKER = 0xff + 0xe2;
        this.JPEG_APP3_MARKER = 0xff + 0xe3;
        this.JPEG_APP4_MARKER = 0xff + 0xe4;
        this.JPEG_APP13_MARKER = 0xff + 0xed;
        this.JPEG_APP14_MARKER = 0xff + 0xee;
        this.JPEG_APP15_MARKER = 0xff + 0xef;
        this.JFIF_MARKER = 0xff + 0xe0;
        this.SOF0_MARKER = 0xff + 0xc0;
        this.SOF1_MARKER = 0xff + 0xc1;
        this.SOF2_MARKER = 0xff + 0xc2;
        this.SOF3_MARKER = 0xff + 0xc3;
        this.DHT_MARKER = 0xff + 0xc4;
        this.SOF5_MARKER = 0xff + 0xc5;
        this.SOF6_MARKER = 0xff + 0xc6;
        this.SOF7_MARKER = 0xff + 0xc7;
        this.SOF8_MARKER = 0xff + 0xc8;
        this.SOF9_MARKER = 0xff + 0xc9;
        this.SOF10_MARKER = 0xff + 0xca;
        this.SOF11_MARKER = 0xff + 0xcb;
        this.DAC_MARKER = 0xff + 0xcc;
        this.SOF13_MARKER = 0xff + 0xcd;
        this.SOF14_MARKER = 0xff + 0xce;
        this.SOF15_MARKER = 0xff + 0xcf;
        this.DRI_MARKER = 0xff + 0xdd;
        this.RST0_MARKER = 0xff + 0xd0;
        this.RST1_MARKER = 0xff + 0xd1;
        this.RST2_MARKER = 0xff + 0xd2;
        this.RST3_MARKER = 0xff + 0xd3;
        this.RST4_MARKER = 0xff + 0xd4;
        this.RST5_MARKER = 0xff + 0xd5;
        this.RST6_MARKER = 0xff + 0xd6;
        this.RST7_MARKER = 0xff + 0xd7;
        this.SOI_MARKER = 0xff + 0xd8;
        this.EOI_MARKER = 0xff + 0xd9;
        this.SOS_MARKER = 0xff + 0xda;
        this.DQT_MARKER = 0xff + 0xdb;
        this.DNL_MARKER = 0xff + 0xdc;
        this.COM_MARKER = 0xff + 0xfe;
        this.MARKERS = [
            this.JFIF_MARKER,
            this.JPEG_APP1_MARKER,
            this.JPEG_APP2_MARKER,
            this.JPEG_APP3_MARKER,
            this.JPEG_APP4_MARKER,
            this.JPEG_APP13_MARKER,
            this.JPEG_APP14_MARKER,
            this.JPEG_APP15_MARKER,
            this.COM_MARKER,
            this.SOF0_MARKER,
            this.SOF1_MARKER,
            this.SOF2_MARKER,
            this.SOF3_MARKER,
            this.DHT_MARKER,
            this.SOF5_MARKER,
            this.SOF6_MARKER,
            this.SOF7_MARKER,
            this.SOF8_MARKER,
            this.SOF9_MARKER,
            this.SOF10_MARKER,
            this.SOF11_MARKER,
            this.DAC_MARKER,
            this.SOF13_MARKER,
            this.SOF14_MARKER,
            this.SOF15_MARKER,
            this.DRI_MARKER,
            this.RST0_MARKER,
            this.RST1_MARKER,
            this.RST2_MARKER,
            this.RST3_MARKER,
            this.RST4_MARKER,
            this.RST5_MARKER,
            this.RST6_MARKER,
            this.RST7_MARKER,
            this.EOI_MARKER,
            this.SOS_MARKER,
            this.DQT_MARKER,
            this.DNL_MARKER,
            this.COM_MARKER,
            this.SOI_MARKER,
            this.JPEG_SOM_MARKER,
            this.XOI,
            this.XOA,
            this.XOT,
        ];
    }
}
