<div class="page-break"></div>

## Chapter 5.2d: Refactoring Code that Accesses External Services (Martin Fowler, 2015)

---

### SECTION 1: PRIMER ON THE BASICS

#### 1. The External Service Coupling Problem

When application code calls external APIs or services directly, it tends to tangle three distinct concerns into one place:

1. **Connection concern** — API authorization, HTTP transport, endpoint URLs
2. **Data structure concern** — parsing the external service's response format into domain data
3. **Domain logic concern** — using that data to compute application results

This tangling makes the code:
- **Hard to test** — you cannot test domain logic without making real network calls
- **Hard to change** — if the external API changes its format, domain logic must be touched
- **Hard to understand** — reading domain logic requires understanding the external API simultaneously

```
          BEFORE REFACTORING: TANGLED CONCERNS

   ┌──────────────────────────────────────────────────────────────┐
   │  VideoService                                                │
   │  ┌────────────────┐  ┌──────────────────┐  ┌─────────────┐ │
   │  │ API Auth &     │  │ YouTube Response │  │ Domain      │ │
   │  │ HTTP Call      ├──┤ Parsing          ├──┤ Logic       │ │
   │  │ (Connection)   │  │ (Data Struct)    │  │ (monthlyViews)│ │
   │  └────────────────┘  └──────────────────┘  └─────────────┘ │
   │          ALL MIXED TOGETHER IN ONE CLASS                    │
   └──────────────────────────────────────────────────────────────┘

          AFTER REFACTORING: SEPARATED CONCERNS

   ┌────────────────┐    ┌──────────────────┐    ┌─────────────┐
   │ YouTubeConnect │    │ YouTubeGateway   │    │ VideoService│
   │ -ion           │───▶│ (data structure  │───▶│ (domain     │
   │ (HTTP/auth)    │    │  translation)    │    │  logic only)│
   └────────────────┘    └──────────────────┘    └─────────────┘
          │                      │                      │
     Tests use real         Tests stub the         Tests only
     or mocked              connection              need the
     connection             layer                  Gateway stub
```

---

#### 2. The Gateway Pattern

The **Gateway** pattern (Fowler, *Patterns of Enterprise Application Architecture*, 2002) wraps access to an external service behind an interface that speaks your application's language — not the external service's API language.

Benefits:
- Domain logic is decoupled from the external format
- The gateway interface can be stubbed in tests — no network calls needed
- If the external API changes, only the gateway implementation changes

---

#### 3. The Seam Concept (Michael Feathers)

A **Seam** is a place in code where you can alter behavior without editing the code at that point. Seams exist at:
- Interface boundaries (dependency injection points)
- Constructor parameters
- Configuration / factory methods

Introducing a gateway interface creates a seam — allowing tests to inject a fake gateway rather than the real one.

---

#### 4. Code Examples — Separating the Connection, Gateway, and Domain

##### Java Implementation
```java
// STEP 1: Define the Gateway interface (the Seam)
interface YouTubeGateway {
    VideoData getVideoData(String youtubeId);
}

record VideoData(String title, long viewCount, LocalDate publishedAt) {}

// STEP 2: Real implementation (connects to YouTube API)
class YouTubeGatewayImpl implements YouTubeGateway {
    private final YouTubeClient client; // wraps HTTP + auth

    public YouTubeGatewayImpl(YouTubeClient client) { this.client = client; }

    @Override
    public VideoData getVideoData(String youtubeId) {
        var response = client.videos().list(youtubeId, "snippet,statistics");
        var item = response.getItems().get(0);
        return new VideoData(
            item.getSnippet().getTitle(),
            Long.parseLong(item.getStatistics().getViewCount()),
            LocalDate.parse(item.getSnippet().getPublishedAt())
        );
    }
}

// STEP 3: Stub for testing (no network call)
class YouTubeGatewayStub implements YouTubeGateway {
    private final Map<String, VideoData> fakeData;
    public YouTubeGatewayStub(Map<String, VideoData> fakeData) { this.fakeData = fakeData; }

    @Override
    public VideoData getVideoData(String youtubeId) {
        return fakeData.getOrDefault(youtubeId, null);
    }
}

// STEP 4: Domain service — uses only the gateway interface, knows nothing about YouTube
class VideoAnalysisService {
    private final YouTubeGateway gateway;

    public VideoAnalysisService(YouTubeGateway gateway) { this.gateway = gateway; }

    public double monthlyViews(String youtubeId) {
        VideoData data = gateway.getVideoData(youtubeId);
        long daysAvailable = ChronoUnit.DAYS.between(data.publishedAt(), LocalDate.now());
        return data.viewCount() * 365.0 / daysAvailable / 12;
    }
}

// STEP 5: Test using stub — no network, fast, deterministic
@Test
void testMonthlyViews() {
    var stub = new YouTubeGatewayStub(Map.of(
        "abc123", new VideoData("Test Video", 12000L, LocalDate.now().minusDays(365))
    ));
    var service = new VideoAnalysisService(stub);
    assertEquals(1000.0, service.monthlyViews("abc123"), 0.1);
}
```

##### JavaScript / TypeScript Implementation
```javascript
// Gateway interface (TypeScript)
interface YouTubeGateway {
  getVideoData(youtubeId: string): Promise<VideoData>;
}

interface VideoData {
  title: string;
  viewCount: number;
  publishedAt: Date;
}

// Real implementation
class YouTubeGatewayImpl implements YouTubeGateway {
  constructor(private readonly apiKey: string) {}

  async getVideoData(youtubeId: string): Promise<VideoData> {
    const resp = await fetch(
      `https://youtube.googleapis.com/youtube/v3/videos?id=${youtubeId}&part=snippet,statistics&key=${this.apiKey}`
    );
    const data = await resp.json();
    const item = data.items[0];
    return {
      title: item.snippet.title,
      viewCount: parseInt(item.statistics.viewCount, 10),
      publishedAt: new Date(item.snippet.publishedAt),
    };
  }
}

// Stub for testing
class YouTubeGatewayStub implements YouTubeGateway {
  constructor(private readonly fakeData: Record<string, VideoData>) {}
  async getVideoData(youtubeId: string): Promise<VideoData> {
    return this.fakeData[youtubeId];
  }
}

// Domain service — depends only on the interface
class VideoAnalysisService {
  constructor(private readonly gateway: YouTubeGateway) {}

  async monthlyViews(youtubeId: string): Promise<number> {
    const data = await this.gateway.getVideoData(youtubeId);
    const daysAvailable = (Date.now() - data.publishedAt.getTime()) / (1000 * 60 * 60 * 24);
    return (data.viewCount * 365) / daysAvailable / 12;
  }
}

// Test
const stub = new YouTubeGatewayStub({
  'abc123': { title: 'Test', viewCount: 12000, publishedAt: new Date('2024-01-01') }
});
const service = new VideoAnalysisService(stub);
const monthly = await service.monthlyViews('abc123');
```

##### Python Implementation
```python
from abc import ABC, abstractmethod
from datetime import date, timedelta
from unittest.mock import MagicMock
from dataclasses import dataclass
from typing import Dict

@dataclass
class VideoData:
    title: str
    view_count: int
    published_at: date

# Gateway interface (abstract base class)
class YouTubeGateway(ABC):
    @abstractmethod
    def get_video_data(self, youtube_id: str) -> VideoData:
        pass

# Real implementation
class YouTubeGatewayImpl(YouTubeGateway):
    def __init__(self, api_key: str):
        self._api_key = api_key

    def get_video_data(self, youtube_id: str) -> VideoData:
        import urllib.request, json
        url = f"https://youtube.googleapis.com/youtube/v3/videos?id={youtube_id}&part=snippet,statistics&key={self._api_key}"
        with urllib.request.urlopen(url) as response:
            data = json.loads(response.read())
        item = data['items'][0]
        return VideoData(
            title=item['snippet']['title'],
            view_count=int(item['statistics']['viewCount']),
            published_at=date.fromisoformat(item['snippet']['publishedAt'][:10])
        )

# Stub for testing
class YouTubeGatewayStub(YouTubeGateway):
    def __init__(self, fake_data: Dict[str, VideoData]):
        self._fake_data = fake_data

    def get_video_data(self, youtube_id: str) -> VideoData:
        return self._fake_data[youtube_id]

# Domain service
class VideoAnalysisService:
    def __init__(self, gateway: YouTubeGateway):
        self._gateway = gateway

    def monthly_views(self, youtube_id: str) -> float:
        data = self._gateway.get_video_data(youtube_id)
        days_available = (date.today() - data.published_at).days
        return data.view_count * 365 / days_available / 12

# Test — no network, deterministic
def test_monthly_views():
    stub = YouTubeGatewayStub({
        'abc123': VideoData('Test', 12000, date.today() - timedelta(days=365))
    })
    service = VideoAnalysisService(stub)
    assert abs(service.monthly_views('abc123') - 1000.0) < 0.1
```

---

<div class="page-break"></div>

### SECTION 2: SYNTHESIZED ACADEMIC SUMMARY

#### 1. Isolating the Domain from the Infrastructure
When refactoring code that interacts with external services, the primary architectural goal is isolation. The core domain logic must be shielded from the volatility, latency, and specific implementation details of third-party APIs, databases, or messaging queues.

#### 2. The Anti-Corruption Layer
A critical pattern in this context is the Anti-Corruption Layer (ACL). By introducing translation interfaces between the external service and the internal domain, the codebase prevents external data models from polluting internal business logic, facilitating easier swapping or upgrading of external dependencies.

#### 3. Handling Failure and Idempotency
Refactoring integrations often involves formalizing how the system handles transient failures. Techniques such as circuit breakers, retries, and ensuring idempotent operations are introduced during refactoring to transform brittle, tightly-coupled integrations into robust, fault-tolerant interactions.

---
