# Podbelly Jukebox Redesign

Direction: **Jukebox** — Energetic, colorful, fun. Vibrant accents on dark surfaces. Horizontal cards, numbered lists, gradient progress, color-coded tags.

## Color Palette

### Dark Theme (Default)

| Role | Hex | Usage |
|------|-----|-------|
| Primary | `#FF6B6B` | Buttons, active nav, progress, links |
| On Primary | `#0F1218` | Text on primary buttons |
| Primary Container | `#3D1A1A` | Tonal button backgrounds |
| On Primary Container | `#FFB4B4` | Text on primary containers |
| Secondary | `#4ECDC4` | Tags, secondary accents |
| On Secondary | `#0A1210` | Text on secondary |
| Secondary Container | `#1A3D3A` | Tonal secondary backgrounds |
| On Secondary Container | `#A8E8E4` | Text on secondary containers |
| Tertiary | `#FFA502` | Duration tags, highlights |
| On Tertiary | `#1A0E00` | Text on tertiary |
| Tertiary Container | `#3D2800` | Tonal tertiary backgrounds |
| On Tertiary Container | `#FFD080` | Text on tertiary containers |
| Error | `#FF6B6B` | Error states (same as primary) |
| On Error | `#0F1218` | Text on error |
| Error Container | `#3D1A1A` | Error backgrounds |
| On Error Container | `#FFB4B4` | Text on error containers |
| Background | `#0A0A12` | Screen background |
| On Background | `#E0E4F0` | Primary text |
| Surface | `#0A0A12` | Same as background |
| On Surface | `#E0E4F0` | Primary text |
| Surface Variant | `#1A1F2E` | Cards, elevated surfaces |
| On Surface Variant | `#8892A8` | Secondary text |
| Outline | `#3A4060` | Borders, dividers |
| Outline Variant | `#252B3D` | Subtle dividers |
| Inverse Surface | `#E0E4F0` | Inverse surfaces |
| Inverse On Surface | `#0F1218` | Text on inverse |
| Inverse Primary | `#C62828` | Primary on inverse |
| Surface Container Low | `#12141E` | Low-elevation cards |
| Surface Container | `#1A1F2E` | Standard cards |
| Surface Container High | `#252B3D` | High-elevation cards, mini-player |

### Light Theme

| Role | Hex |
|------|-----|
| Primary | `#E84848` |
| On Primary | `#FFFFFF` |
| Primary Container | `#FFE0E0` |
| On Primary Container | `#5A0000` |
| Secondary | `#2AAA9F` |
| Tertiary | `#E08800` |
| Background | `#FAFBFF` |
| Surface | `#FAFBFF` |
| Surface Variant | `#F0F2FA` |
| On Surface | `#0F1218` |
| On Surface Variant | `#5A6480` |
| Outline | `#C0C8D8` |

### OLED Dark

Same as dark theme except:
- Background: `#000000`
- Surface: `#000000`
- Surface Container Low: `#0A0A12`
- Surface Container: `#12141E`

### High Contrast

Same as dark theme except:
- Primary: `#FF8080`
- On Surface: `#FFFFFF`
- On Surface Variant: `#FFFFFF`
- Background: `#000000`
- Outline: `#8892A8`

## Typography

Font: **Outfit** (Google Fonts, variable weight 300-800)

| Style | Weight | Size | Letter Spacing | Usage |
|-------|--------|------|----------------|-------|
| Display Large | 800 | 28sp | -0.04em | Brand text |
| Headline Large | 700 | 22sp | -0.03em | Screen titles |
| Headline Medium | 700 | 20sp | -0.02em | Section headers |
| Title Large | 700 | 18sp | -0.02em | Card titles, detail headers |
| Title Medium | 600 | 15sp | 0 | Episode titles |
| Title Small | 600 | 13sp | 0 | Podcast names |
| Body Large | 400 | 15sp | 0.01em | Descriptions |
| Body Medium | 400 | 14sp | 0.01em | Body text |
| Body Small | 400 | 12sp | 0.02em | Secondary body |
| Label Large | 700 | 12sp | 0.12em | Section labels (ALL CAPS) |
| Label Medium | 600 | 11sp | 0.08em | Tags, badges |
| Label Small | 500 | 10sp | 0.04em | Metadata, timestamps |

## Component Design

### Cards
- Border radius: 14dp (main), 10dp (small/artwork)
- Background: surfaceContainer
- Border: 1dp #ffffff06 (dark), 1dp outline (light)
- Active card: 3dp left border in primary color

### Artwork
- Border radius: 14dp (large), 10dp (small)
- Hero artwork: -3deg rotation
- Palette API: extract dominant color for shadow glow
- Shadow: 0dp 6dp 24dp with dominant color at 40% opacity

### Buttons
- Primary: #FF6B6B bg, #0F1218 text, 12dp radius
- Tonal: #FF6B6B20 bg, #FF6B6B text, 12dp radius
- Play (hero): 48dp circle, primary filled
- Play (list): 36dp circle, tonal

### Duration Tags (Color-coded)
- <15min: #4ECDC4 (teal) — "quick listen"
- 15-45min: #FFA502 (amber) — "medium"
- >45min: #FF6B6B (coral) — "deep dive"
- Style: color@10% bg, full-color text, 6dp radius

### Progress Bars
- Gradient: #FF6B6B -> #FFA502
- Height: 4dp (hero), 3dp (card), 2dp (mini-player)
- Track: #ffffff10 (dark), #E0E4F010 (light)

### Bottom Navigation
- Icon + label
- Active: primary color icon + underline bar (16dp wide, 3dp tall)
- Inactive: outline color
- Underline bar animates between tabs with spring physics

### Mini Player
- Background: gradient #1A1F2E -> #252B3D
- 16dp border radius, 8dp margin from edges
- Progress bar at top edge (2dp, gradient)
- Artwork with Palette-extracted glow shadow

### Now Playing Hero Card
- Full-width, gradient background (#1A1530 -> #0F1828)
- Pulsing "live" dot next to "NOW PLAYING" label
- Tilted artwork (-3deg) with colored shadow
- Gradient progress bar

## Screen Designs

### Home Screen
- Brand header: "pod**belly**" (belly in primary) + avatar
- Now Playing hero card (if playing)
- "Recently Added" horizontal card carousel (130dp cards)
- Episode list with duration color tags
- Pull-to-refresh with custom indicator

### Player Screen
- Palette API background tinting from artwork
- Large artwork (300dp) with colored glow, slight float
- Gradient progress bar
- Transport controls with animated states
- Speed/sleep/silence as tonal chips
- Bottom sheets tinted with artwork colors

### Podcast Detail Screen
- Header: 120dp artwork with glow + gradient overlay
- Outfit Bold title, lighter-weight author
- Expandable description (animateContentSize)
- Filter chips with Jukebox tonal styling
- Episodes with duration color tags

### Library Screen
- Grid: cards with artwork fill, title overlaid with gradient
- List: compact rows with episode count badge
- Sort/filter as horizontal tonal chips

### Discover Screen
- Pill-shaped search bar (28dp radius)
- Results as cards (same component system)
- RSS input styled as gradient-bordered card

### Downloads Screen
- Same card system as home
- Failed: error card with primary left border + retry
- Progress: animated gradient bar

### Settings Screen
- All-caps label headers with wide tracking
- Cards with 14dp radius
- Coral switches and sliders
- Stats link prominent

### Stats Screen
- Hero stat cards with gradient backgrounds
- Podcast leaderboard with ranks and artwork

### Splash Screen
- "pod" fades in (400ms)
- "belly" slides in from right with spring physics (coral)
- Hold 200ms
- Fade out (300ms) to home

## Animations

### Page Transitions
- Enter: slide up (200ms) + fade in, spring dampingRatio=0.8f
- Exit: fade out (150ms)
- Tab switch: cross-fade (200ms)

### List Animations
- Staggered reveal: items animate in with 50ms delay, slide up + fade
- First load only
- Carousel: cards scale 0.95 -> 1.0 on viewport enter

### Micro-interactions
- Play press: scale 1.0 -> 0.9 -> 1.0 (spring)
- Download: icon morphs to circular progress (animated vector)
- Subscribe: icon scales up with overshoot spring
- Pull-to-refresh: custom rotation

### Progress
- Gradient shimmer: subtle position shift while playing
- Mini-player: animateFloatAsState transitions

### Navigation
- Mini-player: spring bounce slide up + fade
- Bottom nav: underline bar slides with spring physics
- Active indicator: animateOffsetAsState

### Hero Card
- Pulsing dot: infinite opacity 1.0->0.3->1.0 (2s)
- Artwork wobble on press: -3deg -> 0deg -> -3deg (spring)

### Haptics
- Light: play/pause, pull-to-refresh threshold
- Medium: subscribe/unsubscribe
