package androidx.compose.foundation.text;

import androidx.compose.ui.unit.IntOffset;
import androidx.media3.extractor.text.ttml.TtmlNode;
import com.arthenica.ffmpegkit.StreamInformation;
import kotlin.Metadata;
import kotlin.jvm.functions.Function0;
/* compiled from: TextLinkScope.kt */
@Metadata(d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002J$\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t¨\u0006\u000b"}, d2 = {"Landroidx/compose/foundation/text/TextRangeLayoutMeasureScope;", "", "()V", TtmlNode.TAG_LAYOUT, "Landroidx/compose/foundation/text/TextRangeLayoutMeasureResult;", StreamInformation.KEY_WIDTH, "", StreamInformation.KEY_HEIGHT, "place", "Lkotlin/Function0;", "Landroidx/compose/ui/unit/IntOffset;", "foundation_release"}, k = 1, mv = {1, 8, 0}, xi = 48)
/* loaded from: classes.dex */
public final class TextRangeLayoutMeasureScope {
    public static final int $stable = 0;

    public final TextRangeLayoutMeasureResult layout(int width, int height, Function0<IntOffset> function0) {
        return new TextRangeLayoutMeasureResult(width, height, function0);
    }
}
