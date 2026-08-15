package com.zamp.invoice.extraction;

import java.awt.Rectangle;

public record OcrWord(
        String text,
        double confidence,
        Rectangle boundingBox
) {
}
