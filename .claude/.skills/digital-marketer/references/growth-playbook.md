# Digital Marketer — Growth Playbook & Campaign Reference

Load when designing a launch strategy, setting up analytics, or running growth campaigns.

---

## AARRR Funnel (Pirate Metrics)

```
Acquisition  → How do users find us?    KPI: traffic, CAC, channel mix
Activation   → Do they have a good UX?  KPI: sign-up rate, onboarding completion
Retention    → Do they come back?        KPI: DAU/MAU, churn rate, cohort retention
Revenue      → Do they pay?              KPI: MRR, ARPU, LTV
Referral     → Do they tell others?      KPI: NPS, referral rate, viral coefficient
```

Fix retention before scaling acquisition. Pouring traffic into a leaky bucket wastes budget.

---

## Channel Prioritization Matrix

| Channel | Best For | Time to Results | Cost |
|---|---|---|---|
| SEO | Long-term organic | 3-6 months | Low (time) |
| Content marketing | Awareness, trust | 2-4 months | Low-Med |
| Paid search (Google) | High-intent buyers | Days | High CPC |
| Paid social (Meta/LinkedIn) | Awareness, targeting | Days-weeks | Med |
| Email marketing | Retention, re-engagement | Days | Very low |
| Product-led growth | B2B SaaS, freemium | Weeks | Low |
| Partnerships / affiliates | Niche audiences | Weeks-months | Revenue share |

---

## SEO Checklist

### Technical
- [ ] Page speed < 2s LCP (Core Web Vitals — Google ranking factor)
- [ ] Mobile-first — passes Google Mobile-Friendly Test
- [ ] HTTPS on all pages
- [ ] XML sitemap submitted to Google Search Console
- [ ] `robots.txt` not blocking important pages
- [ ] No broken internal links (404s)
- [ ] Canonical tags on duplicate content pages

### On-Page
- [ ] Target keyword in `<title>` (< 60 chars)
- [ ] Target keyword in meta description (< 160 chars, compelling)
- [ ] One `<h1>` per page (contains keyword)
- [ ] Subheadings (`h2`, `h3`) include secondary keywords
- [ ] Images have descriptive `alt` text
- [ ] Internal links to related content

### Content
- [ ] Content answers the search intent (informational / transactional / navigational)
- [ ] Minimum 1,000 words for competitive terms
- [ ] Updated within last 12 months (freshness signal)
- [ ] Outbound links to authoritative sources

---

## Email Marketing Templates

### Welcome Sequence (5 emails)
```
Day 0: Welcome + what to expect + key resource
Day 1: Quick win — help them get their first value
Day 3: Social proof — customer story / case study
Day 7: Address top objection (cost, complexity, time)
Day 14: CTA — upgrade / book demo / invite a teammate
```

### Re-engagement Campaign
```
Segment: users inactive for 60 days
Email 1: "We miss you" — show what's new
Email 2: "Here's something just for you" — exclusive offer
Email 3: "Last chance" — clear opt-out path (clean the list)
```

---

## Campaign Performance Metrics

| Metric | Industry Benchmark | Action Threshold |
|---|---|---|
| Email open rate | 20-25% | < 15% → review subject lines |
| Email CTR | 2-3% | < 1% → review CTA and content |
| Landing page CVR | 2-5% | < 2% → A/B test |
| Google Ads CTR | 2-5% | < 1% → review ad copy |
| CAC (B2B SaaS) | < LTV × 0.33 | CAC > LTV/3 → fix funnel |
| NPS | > 30 good, > 50 excellent | < 0 → churn risk |

---

## Landing Page Conversion Checklist

```
Above the fold:
[ ] Clear value proposition (who it's for + what they get)
[ ] Primary CTA visible without scrolling
[ ] No navigation menu (reduces distractions on landing pages)

Trust signals:
[ ] Customer logos or testimonials
[ ] Number of users / reviews / rating
[ ] Security badges (if payments involved)

CTA:
[ ] Specific action verb: "Start free trial" not "Submit"
[ ] Low commitment framing: "No credit card required"
[ ] Repeated at bottom of page

Form:
[ ] Minimum fields (email only if possible)
[ ] Privacy note near submit button
```

---

## UTM Tagging Convention

```
utm_source   = traffic source     (google, facebook, newsletter, partner)
utm_medium   = marketing medium   (cpc, email, organic, social, referral)
utm_campaign = campaign name      (q3-launch, black-friday, webinar-sep)
utm_content  = ad variant         (hero-cta, sidebar-banner)
utm_term     = keyword (paid)     (project-management-software)

Example:
https://app.example.com/?utm_source=google&utm_medium=cpc&utm_campaign=q3-launch
```

Always use UTMs on paid and social links — never on internal links.

---

## Analytics Setup Checklist

```
[ ] Google Analytics 4 or Amplitude installed
[ ] Conversion events tracked (sign-up, purchase, activation)
[ ] Funnel steps mapped and tracked
[ ] UTM parameters flowing into analytics
[ ] Revenue data connected (Stripe → analytics)
[ ] Weekly dashboard with AARRR metrics
[ ] Cohort retention chart set up (weekly/monthly)
```
