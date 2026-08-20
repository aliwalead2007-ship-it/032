package com.example

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

/**
 * محرك رسوم Lottie التفاعلية لحالات العقل الإخراجي (StyleBrain)
 * يوفر مؤشرات بصرية حية مبنية بمكتبة Lottie Compose متفاعلة مع نبضات التفكير والاستقرار والنجاح.
 */
object QabasLottieAnimations {

    /**
     * رسم Lottie JSON: نبضات التفكير العصبي والاستدلال النشط (Active Neural Thinking & Gemini Reasoning)
     * حلقات ذهبية وسماوية متحدة المركز تتوسع وتنبض مع جزيئات عصبية دوارة.
     */
    val BRAIN_THINKING_JSON = """
    {
      "v": "5.5.7",
      "fr": 60,
      "ip": 0,
      "op": 120,
      "w": 300,
      "h": 300,
      "nm": "BrainThinkingPulse",
      "ddd": 0,
      "assets": [],
      "layers": [
        {
          "ddd": 0,
          "ind": 1,
          "ty": 4,
          "nm": "CoreEnergy",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [80] }, { "t": 60, "s": [100] }, { "t": 120, "s": [80] }] },
            "r": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 120, "s": [360] }] },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [90, 90, 100] }, { "t": 60, "s": [115, 115, 100] }, { "t": 120, "s": [90, 90, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "el",
                  "p": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [60, 60] }
                },
                {
                  "ty": "fl",
                  "c": { "a": 0, "k": [0.91, 0.77, 0.28, 1] },
                  "o": { "a": 0, "k": 100 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 2,
          "ty": 4,
          "nm": "InnerPulseRing",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [90] }, { "t": 60, "s": [40] }, { "t": 120, "s": [90] }] },
            "r": { "a": 1, "k": [{ "t": 0, "s": [360] }, { "t": 120, "s": [0] }] },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [80, 80, 100] }, { "t": 60, "s": [140, 140, 100] }, { "t": 120, "s": [80, 80, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "el",
                  "p": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [110, 110] }
                },
                {
                  "ty": "st",
                  "c": { "a": 0, "k": [0.22, 0.74, 0.97, 1] },
                  "o": { "a": 0, "k": 100 },
                  "w": { "a": 0, "k": 4 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 3,
          "ty": 4,
          "nm": "OuterWaveExpansion",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [100] }, { "t": 90, "s": [20] }, { "t": 120, "s": [0] }] },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [50, 50, 100] }, { "t": 120, "s": [200, 200, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "el",
                  "p": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [140, 140] }
                },
                {
                  "ty": "st",
                  "c": { "a": 0, "k": [0.96, 0.84, 0.43, 1] },
                  "o": { "a": 0, "k": 80 },
                  "w": { "a": 0, "k": 3 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        }
      ]
    }
    """.trimIndent()

    /**
     * رسم Lottie JSON: استقرار وهدوء العقل عند الجاهزية (Stable & Ready Harmonic Aura)
     * هالة ناعمة وهادئة تتنفس بانتظام وثقة ملكية.
     */
    val BRAIN_IDLE_READY_JSON = """
    {
      "v": "5.5.7",
      "fr": 60,
      "ip": 0,
      "op": 120,
      "w": 300,
      "h": 300,
      "nm": "BrainIdleReady",
      "ddd": 0,
      "assets": [],
      "layers": [
        {
          "ddd": 0,
          "ind": 1,
          "ty": 4,
          "nm": "StableCore",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [85] }, { "t": 60, "s": [100] }, { "t": 120, "s": [85] }] },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [95, 95, 100] }, { "t": 60, "s": [105, 105, 100] }, { "t": 120, "s": [95, 95, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "el",
                  "p": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [50, 50] }
                },
                {
                  "ty": "fl",
                  "c": { "a": 0, "k": [0.91, 0.77, 0.28, 1] },
                  "o": { "a": 0, "k": 90 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 2,
          "ty": 4,
          "nm": "GentleAura",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [40] }, { "t": 60, "s": [70] }, { "t": 120, "s": [40] }] },
            "r": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 120, "s": [90] }] },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [100, 100, 100] }, { "t": 60, "s": [118, 118, 100] }, { "t": 120, "s": [100, 100, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "el",
                  "p": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] }
                },
                {
                  "ty": "st",
                  "c": { "a": 0, "k": [0.91, 0.77, 0.28, 1] },
                  "o": { "a": 0, "k": 70 },
                  "w": { "a": 0, "k": 2.5 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        }
      ]
    }
    """.trimIndent()

    /**
     * رسم Lottie JSON: اكتمال واتخاذ القرار الإخراجي بنجاح (Decision Success Burst)
     * وميض ذهبي وأخضر زمردي احتفالي بالنجاح والتوافق العالي.
     */
    val BRAIN_SUCCESS_JSON = """
    {
      "v": "5.5.7",
      "fr": 60,
      "ip": 0,
      "op": 120,
      "w": 300,
      "h": 300,
      "nm": "BrainSuccessBurst",
      "ddd": 0,
      "assets": [],
      "layers": [
        {
          "ddd": 0,
          "ind": 1,
          "ty": 4,
          "nm": "SuccessCheckCore",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 100 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [50, 50, 100] }, { "t": 30, "s": [120, 120, 100] }, { "t": 50, "s": [100, 100, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "el",
                  "p": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [70, 70] }
                },
                {
                  "ty": "fl",
                  "c": { "a": 0, "k": [0.06, 0.72, 0.51, 1] },
                  "o": { "a": 0, "k": 100 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 2,
          "ty": 4,
          "nm": "GoldBurstRing",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [100] }, { "t": 60, "s": [0] }] },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [40, 40, 100] }, { "t": 60, "s": [220, 220, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "el",
                  "p": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [120, 120] }
                },
                {
                  "ty": "st",
                  "c": { "a": 0, "k": [0.91, 0.77, 0.28, 1] },
                  "o": { "a": 0, "k": 100 },
                  "w": { "a": 0, "k": 4 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        }
      ]
    }
    """.trimIndent()

    /**
     * رسم Lottie JSON: احتفالية وتوهج دخول استوديو الإنتاج الإبداعي (Studio Entry Cinematic Portal)
     * عدسة ذهبية دوارة مع نجمة إسلامية ثمانية الأبعاد وهالات إشعاع وجزيئات ضوئية متصاعدة.
     */
    val STUDIO_ENTRY_PORTAL_JSON = """
    {
      "v": "5.5.7",
      "fr": 60,
      "ip": 0,
      "op": 180,
      "w": 300,
      "h": 300,
      "nm": "StudioEntryPortal",
      "ddd": 0,
      "assets": [],
      "layers": [
        {
          "ddd": 0,
          "ind": 1,
          "ty": 4,
          "nm": "CentralLightSpark",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 40, "s": [100] }, { "t": 90, "s": [85] }, { "t": 180, "s": [100] }] },
            "r": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 180, "s": [720] }] },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [20, 20, 100] }, { "t": 50, "s": [115, 115, 100] }, { "t": 110, "s": [95, 95, 100] }, { "t": 180, "s": [110, 110, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sr",
                  "p": { "a": 0, "k": [0, 0] },
                  "r": { "a": 0, "k": 0 },
                  "pt": { "a": 0, "k": 8 },
                  "ir": { "a": 0, "k": 22 },
                  "is": { "a": 0, "k": 0 },
                  "or": { "a": 0, "k": 45 },
                  "os": { "a": 0, "k": 0 },
                  "sy": 2
                },
                {
                  "ty": "fl",
                  "c": { "a": 0, "k": [0.96, 0.84, 0.43, 1] },
                  "o": { "a": 0, "k": 95 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 2,
          "ty": 4,
          "nm": "RotatingLensAperture",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 30, "s": [90] }, { "t": 180, "s": [70] }] },
            "r": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 180, "s": [-360] }] },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [50, 50, 100] }, { "t": 60, "s": [120, 120, 100] }, { "t": 180, "s": [105, 105, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "el",
                  "p": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [110, 110] }
                },
                {
                  "ty": "st",
                  "c": { "a": 0, "k": [0.85, 0.72, 0.28, 1] },
                  "o": { "a": 0, "k": 90 },
                  "w": { "a": 0, "k": 3.5 },
                  "d": [{ "n": "d", "v": { "a": 0, "k": 18 } }, { "n": "g", "v": { "a": 0, "k": 10 } }]
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 3,
          "ty": 4,
          "nm": "ExpandingRadiantWave",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 20, "s": [80] }, { "t": 120, "s": [20] }, { "t": 180, "s": [0] }] },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [150, 150, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [40, 40, 100] }, { "t": 180, "s": [230, 230, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "el",
                  "p": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [120, 120] }
                },
                {
                  "ty": "st",
                  "c": { "a": 0, "k": [0.91, 0.77, 0.28, 1] },
                  "o": { "a": 0, "k": 60 },
                  "w": { "a": 0, "k": 2 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 4,
          "ty": 4,
          "nm": "FloatingGoldSpark1",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 40, "s": [100] }, { "t": 140, "s": [20] }, { "t": 180, "s": [0] }] },
            "r": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 180, "s": [180] }] },
            "p": { "a": 1, "k": [{ "t": 0, "s": [150, 150, 0] }, { "t": 180, "s": [80, 70, 0] }] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [10, 10, 100] }, { "t": 60, "s": [100, 100, 100] }, { "t": 180, "s": [20, 20, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sr",
                  "p": { "a": 0, "k": [0, 0] },
                  "r": { "a": 0, "k": 0 },
                  "pt": { "a": 0, "k": 4 },
                  "ir": { "a": 0, "k": 4 },
                  "is": { "a": 0, "k": 0 },
                  "or": { "a": 0, "k": 14 },
                  "os": { "a": 0, "k": 0 },
                  "sy": 2
                },
                {
                  "ty": "fl",
                  "c": { "a": 0, "k": [1.0, 0.92, 0.65, 1] },
                  "o": { "a": 0, "k": 90 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 5,
          "ty": 4,
          "nm": "FloatingGoldSpark2",
          "sr": 1,
          "ks": {
            "o": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 60, "s": [100] }, { "t": 160, "s": [20] }, { "t": 180, "s": [0] }] },
            "r": { "a": 1, "k": [{ "t": 0, "s": [0] }, { "t": 180, "s": [-180] }] },
            "p": { "a": 1, "k": [{ "t": 0, "s": [150, 150, 0] }, { "t": 180, "s": [225, 85, 0] }] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [10, 10, 100] }, { "t": 70, "s": [90, 90, 100] }, { "t": 180, "s": [20, 20, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                {
                  "ty": "sr",
                  "p": { "a": 0, "k": [0, 0] },
                  "r": { "a": 0, "k": 0 },
                  "pt": { "a": 0, "k": 4 },
                  "ir": { "a": 0, "k": 3 },
                  "is": { "a": 0, "k": 0 },
                  "or": { "a": 0, "k": 11 },
                  "os": { "a": 0, "k": 0 },
                  "sy": 2
                },
                {
                  "ty": "fl",
                  "c": { "a": 0, "k": [0.96, 0.84, 0.43, 1] },
                  "o": { "a": 0, "k": 90 }
                },
                {
                  "ty": "tr",
                  "p": { "a": 0, "k": [0, 0] },
                  "a": { "a": 0, "k": [0, 0] },
                  "s": { "a": 0, "k": [100, 100] },
                  "r": { "a": 0, "k": 0 },
                  "o": { "a": 0, "k": 100 }
                }
              ]
            }
          ]
        }
      ]
    }
    """.trimIndent()

    /**
     * رسم Lottie JSON: تموج الذبذبات الصوتية الذكية للاستوديو (Studio Mic Live Waveform)
     */
    val STUDIO_MIC_WAVE_JSON = """
    {
      "v": "5.5.7",
      "fr": 60,
      "ip": 0,
      "op": 60,
      "w": 100,
      "h": 100,
      "nm": "StudioMicWave",
      "ddd": 0,
      "assets": [],
      "layers": [
        {
          "ddd": 0,
          "ind": 1,
          "ty": 4,
          "nm": "Bar1",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 100 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [25, 50, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [100, 40, 100] }, { "t": 30, "s": [100, 100, 100] }, { "t": 60, "s": [100, 40, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "ty": "rc", "p": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [8, 40] }, "r": { "a": 0, "k": 4 } },
                { "ty": "fl", "c": { "a": 0, "k": [0.12, 0.16, 0.24, 1] }, "o": { "a": 0, "k": 100 } },
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 2,
          "ty": 4,
          "nm": "Bar2",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 100 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [50, 50, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [100, 90, 100] }, { "t": 25, "s": [100, 45, 100] }, { "t": 60, "s": [100, 90, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "ty": "rc", "p": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [8, 55] }, "r": { "a": 0, "k": 4 } },
                { "ty": "fl", "c": { "a": 0, "k": [0.12, 0.16, 0.24, 1] }, "o": { "a": 0, "k": 100 } },
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ]
            }
          ]
        },
        {
          "ddd": 0,
          "ind": 3,
          "ty": 4,
          "nm": "Bar3",
          "sr": 1,
          "ks": {
            "o": { "a": 0, "k": 100 },
            "r": { "a": 0, "k": 0 },
            "p": { "a": 0, "k": [75, 50, 0] },
            "a": { "a": 0, "k": [0, 0, 0] },
            "s": { "a": 1, "k": [{ "t": 0, "s": [100, 50, 100] }, { "t": 35, "s": [100, 100, 100] }, { "t": 60, "s": [100, 50, 100] }] }
          },
          "shapes": [
            {
              "ty": "gr",
              "it": [
                { "ty": "rc", "p": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [8, 40] }, "r": { "a": 0, "k": 4 } },
                { "ty": "fl", "c": { "a": 0, "k": [0.12, 0.16, 0.24, 1] }, "o": { "a": 0, "k": 100 } },
                { "ty": "tr", "p": { "a": 0, "k": [0, 0] }, "a": { "a": 0, "k": [0, 0] }, "s": { "a": 0, "k": [100, 100] }, "r": { "a": 0, "k": 0 }, "o": { "a": 0, "k": 100 } }
              ]
            }
          ]
        }
      ]
    }
    """.trimIndent()
}

/**
 * مكون بصري متفاعل بمكتبة Lottie يعرض حالة العقل الإخراجي
 * يتحكم في سرعة النبض والنمط الحركي تلقائياً بناءً على BrainStatus
 */
@Composable
fun LottieBrainVisualizer(
    status: BrainStatus,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val jsonString = remember(status) {
        when (status) {
            BrainStatus.IDLE -> QabasLottieAnimations.BRAIN_IDLE_READY_JSON
            BrainStatus.SUCCESS -> QabasLottieAnimations.BRAIN_SUCCESS_JSON
            else -> QabasLottieAnimations.BRAIN_THINKING_JSON
        }
    }

    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.JsonString(jsonString)
    )

    val speed = when (status) {
        BrainStatus.GEMINI_REASONING -> 2.2f
        BrainStatus.ANALYZING_IDEA, BrainStatus.MATCHING_STYLE -> 1.7f
        BrainStatus.FETCHING_MEMORY, BrainStatus.SYNTHESIZING_DIRECTIVES -> 1.3f
        BrainStatus.SUCCESS -> 1.0f
        BrainStatus.IDLE -> 0.8f
        BrainStatus.ERROR -> 0.6f
    }

    val iterations = if (status == BrainStatus.SUCCESS) 2 else LottieConstants.IterateForever

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed,
        isPlaying = true
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * حركة دخول استوديو قبس الإبداعي (Lottie Studio Entry Portal)
 * تظهر ببراعة سينمائية وهندسة إسلامية ذهبية ترحيباً بصانع المحتوى.
 */
@Composable
fun LottieStudioEntryHero(
    modifier: Modifier = Modifier,
    size: Dp = 90.dp,
    speed: Float = 1.0f,
    iterations: Int = LottieConstants.IterateForever
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.JsonString(QabasLottieAnimations.STUDIO_ENTRY_PORTAL_JSON)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed,
        isPlaying = true
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * ويدجت ذبذبات الميكروفون المباشرة المبنية بـ Lottie لزر صناعة المحتوى الصوتي
 */
@Composable
fun LottieStudioMicWave(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.JsonString(QabasLottieAnimations.STUDIO_MIC_WAVE_JSON)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1.2f,
        isPlaying = true
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

