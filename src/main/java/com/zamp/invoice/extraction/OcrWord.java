package com.zamp.invoice.extraction;

import java.awt.Rectangle;

public record OcrWord(
        String text,
        float confidence,
        Rectangle boundingBox
) {
}
