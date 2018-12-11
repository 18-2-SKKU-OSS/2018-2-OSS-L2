package org.empyrn.darkknight;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v4.util.LruCache;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public class TypefaceSpan extends MetricAffectingSpan {
    /** An <code>LruCache</code> for previously loaded typefaces. */
    /** 이전에 로드된 typefaces를 위한 <code>LruCache</code> */
  private static LruCache<String, Typeface> sTypefaceCache =
          new LruCache<String, Typeface>(12);

  private Typeface mTypeface;

  /**
   * Load the {@link Typeface} and apply to a {@link Spannable}.
   * 객체 Typeface 를 불러와 Spnnable에 적용한다.
   */
  public TypefaceSpan(Context context, String typefaceName) {
      mTypeface = sTypefaceCache.get(typefaceName);
      // If the Typeface is not cached, it is called by the asset and cached.
      // Typeface가 캐시되어 있지 않은 경우 asset에서 불러와 캐시한다.
      if (mTypeface == null) {
          mTypeface = Typeface.createFromAsset(context.getApplicationContext()
                  .getAssets(), String.format("fonts/%s", typefaceName));

          // Cache the loaded Typeface
          // 로드된 Typeface를 캐시한다.
          sTypefaceCache.put(typefaceName, mTypeface);
      }
  }

  @Override
  public void updateMeasureState(TextPaint p) {
      p.setTypeface(mTypeface);

      // Note: This flag is required for proper typeface rendering
      // 주목: 이 플래그는 적절한 typeface 렌더링을 필요로 합니다.
      p.setFlags(p.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
  }

  @Override
  public void updateDrawState(TextPaint tp) {
      tp.setTypeface(mTypeface);

      // Note: This flag is required for proper typeface rendering
      // 주목: 이 플래그는 적절한 typeface 렌더링을 필요로 합니다.
      tp.setFlags(tp.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
  }
}
