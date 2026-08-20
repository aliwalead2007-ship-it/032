package androidx.appcompat.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
/* loaded from: classes.dex */
class ActionBarBackgroundDrawable extends Drawable {
    final ActionBarContainer mContainer;

    public ActionBarBackgroundDrawable(ActionBarContainer container) {
        this.mContainer = container;
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        boolean z = this.mContainer.mIsSplit;
        ActionBarContainer actionBarContainer = this.mContainer;
        if (z) {
            if (actionBarContainer.mSplitBackground != null) {
                this.mContainer.mSplitBackground.draw(canvas);
                return;
            }
            return;
        }
        if (actionBarContainer.mBackground != null) {
            this.mContainer.mBackground.draw(canvas);
        }
        if (this.mContainer.mStackedBackground != null && this.mContainer.mIsStacked) {
            this.mContainer.mStackedBackground.draw(canvas);
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int alpha) {
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter cf) {
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return 0;
    }

    @Override // android.graphics.drawable.Drawable
    public void getOutline(Outline outline) {
        boolean z = this.mContainer.mIsSplit;
        ActionBarContainer actionBarContainer = this.mContainer;
        if (z) {
            if (actionBarContainer.mSplitBackground != null) {
                Api21Impl.getOutline(this.mContainer.mBackground, outline);
            }
        } else if (actionBarContainer.mBackground != null) {
            Api21Impl.getOutline(this.mContainer.mBackground, outline);
        }
    }

    /* loaded from: classes.dex */
    private static class Api21Impl {
        private Api21Impl() {
        }

        public static void getOutline(Drawable drawable, Outline outline) {
            drawable.getOutline(outline);
        }
    }
}
