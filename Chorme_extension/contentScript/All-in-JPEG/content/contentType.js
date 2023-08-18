 const ContentType = {
    Image: 'Image',
    Audio: 'Audio',
    Text: 'Text'
};

 const ContentAttribute = {
    none: { code: 0 },
    basic: { code: 1 },
    burst: { code: 2 },
    object_focus: { code: 3 },
    distance_focus: { code: 4 },
    magic: { code: 5 },
    edited: { code: 6 },

    fromCode: function(code) {
        for (const attr in this) {
            if (this[attr].code === code) {
                return attr;
            }
        }
        return this.none;
    }
};
export default ContentAttribute;