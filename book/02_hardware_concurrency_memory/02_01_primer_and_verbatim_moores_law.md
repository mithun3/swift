<div class="page-break"></div>

# Chapter 2.2: Moore's Law — Past, Present, and Future (Robert R. Schaller & Gordon E. Moore)

---

## SECTION 1: PRIMER ON THE BASICS

### 1. The Origin of Moore's Law (1965 & 1975)
In 1965, Gordon E. Moore (then R&D Director at Fairchild Semiconductor and later co-founder of Intel) was asked by *Electronics* magazine to predict the future of the semiconductor industry over the next decade. 

Plotting component counts from just three data points on a log-linear scale, Moore observed that the number of components per integrated circuit (IC) doubled every year:

```
               MOORE'S LAW COMPONENT DOUBLING (1965 - 1975)
               
  Density (Components / IC)
   65,000 │                                                 * (1975 Target)
          │                                           *
          │                                     *
          │                               *
       64 │                         * (1965 Lab IC)
       32 │                   * (1964 Commercial IC)
        1 │             * (1959 First Planar Transistor)
          └─────────────┴─────┴─────┴─────┴─────┴─────┴─────┴──────
                       1959  1964  1965                     1975
```

In 1975, Moore revised his estimate to a doubling every **18 to 24 months**, which accurately tracked semiconductor progress for over 40 years.

### 2. The Three Factors Driving Density Doubling
Gordon Moore identified three distinct engineering factors that enabled density doubling:
1. **Finer Line Widths / Feature Sizes**: Photolithographic advances reduced transistor gate dimensions.
2. **Larger Die Sizes**: Wafer manufacturing allowed larger silicon dice without incurring fatal defect rates.
3. **Circuit & Device "Cleverness"**: Ingenious circuit layouts, isolation techniques, and component packing (which Moore noted reached physical limits around 1975).

### 3. Software Demand & Myhrvold's Law
Moore's Law was reinforced by a massive positive feedback loop from software. Nathan Myhrvold (former CTO of Microsoft) observed that **software complexity grows faster than hardware capability**. As fast CPUs emerged, software engineers expanded capabilities, bloat, and features—which in turn created an insatiable market demand for even faster processors.

### 4. The Breakdown of Dennard Scaling
Historically, as transistors shrank, their power density remained constant, meaning newer chips ran faster without using more power (Dennard Scaling). Around 2005, this scaling law collapsed due to physical current leakage at sub-micron scales.

### 5. Moore's Second Law (Rock's Law)
While the cost per transistor decreases exponentially, the capital cost of building a state-of-the-art semiconductor fabrication plant (Fab) increases exponentially:
```text
          FAB CAPITAL COST OVER TIME (ROCK'S LAW)
          
  Cost (Billions USD)
   $20B │                                       * (2024 TSMC 2nm)
        │
   $10B │                               * (2015 14nm)
        │
    $5B │                       * (2005 65nm)
        │
  $1.5B │               * (1995)
        │
 $0.01B │       * (1966)
        └───────┴───────┴───────┴───────┴───────┴───────
              1966    1995    2005    2015    2024
```
This economic barrier led to the "Fabless / Foundry" model, where only a few behemoths (TSMC, Intel, Samsung) can afford to build cutting-edge physical fabs.

---

<div class="page-break"></div>

### SECTION 2: VERBATIM RESEARCH PAPER

#### Moore's Law: Past, Present, and Future (June 1997)
*By Robert R. Schaller (Published in IEEE Spectrum, Vol. 34, No. 6, pp. 52-59)*

In 1965 Gordon E. Moore, then R&D director at Fairchild Semiconductor and these days chairman emeritus of Intel Corp., Santa Clara, Calif., quantified the astounding growth of the new technology of semiconductors in a still more astounding formula. Manufacturers, he said, had been doubling the density of components per integrated circuit at regular intervals, and they would continue to do so as far as the eye could see.

This observation has since been dubbed “Moore’s Law” and is now enormously influential. Some have even termed it a self-fulfilling prophecy. Because of the accuracy with which Moore’s Law has predicted past growth in IC complexity, it is viewed as a reliable method of calculating future trends as well, setting the pace of innovation, and defining the rules and the very nature of competition. And since the semiconductor portion of electronic consumer products keeps growing by leaps and bounds, the Law has aroused in users and consumers an expectation of a continuous stream of faster, better, and cheaper high-technology products. Even the policy implications of Moore’s Law are significant: it is used as the baseline assumption in the industry’s strategic road map for the next decade and a half.

Besides its surprising longevity as a forecaster of hardware capabilities, the Law has set the pace for the PC-software industry and, some say, is now a techno-mantra that, if repeated often enough and sincerely enough, has the power of a self-fulfilling prophecy.

#### Genesis of the IC
The 1947 invention of the transfer resistor, or transistor, by William Shockley and his colleagues at Bell Laboratories, Murray Hill, N.J., ushered in the solid-state era of electronics. The concept was based on the discovery that the flow of electricity through a solid such as silicon can be controlled by adding impurities with the appropriate electronic configurations. The vacuum tube was the dominant technology for this task at the time; but the transistor proved to be significantly more reliable, required much less power, and above all, could be made incredibly smaller.

Almost a decade later, Shockley and two Bell Labs colleagues, John Bardeen and Walter Brattain, were presented with the Nobel Prize for their invention. Shockley went on to start his own semiconductor laboratory, and others from his team either joined or founded the companies whose names are synonymous with the spectacular rise of the semiconductor industry: Texas Instruments, Fairchild Semiconductor, and Intel. Gordon Moore, a member of Shockley’s team, was a key player at Fairchild and cofounder of both Fairchild and Intel.

In the late 1950s, research engineers at Fairchild developed the first planar transistor, and later the first planar IC. (Jack Kilby of Texas Instruments is credited with inventing the IC.) Although not as significant a scientific breakthrough as the transistor, the invention of the IC did reveal the potential for extending the cost and operating benefits of transistors to every mass-produced electronic circuit.

#### From Science to Production Technology
Revolutionary science supplied the theoretical basis for solid-state electronics, but without the invention of unprecedented production technologies, the spectacular growth of the industry could not have happened. “Indeed, the technology led the science in a sort of inverse linear model,” Moore observed in a recent publication. Conventionally, science discovers and technology applies; here the order was reversed. The two most noteworthy inventions were the diffusion and oxide-masking process and the planar process.

The planar process was the brainchild of physicist Jean Hoerni of newly formed Fairchild Semiconductor. Hoerni reasoned that a design based on a plane would be easier to manufacture and to miniaturize, compared to the conventional 3-D mesa transistor. Flattening the mesa enabled electrical connections to be made by evaporating metal film onto appropriate regions of the semiconductor wafer. Like the printing process itself, the planar process evolved into ever greater rates of production at even higher yields.

Better still, the planar process enabled the integration of circuits on a single substrate. Robert Noyce at Fairchild quickly recognized this. “When we were patenting this [planar transistor],” recalled Moore, “we recognized it was a significant change... Bob Noyce got a group together to see what they could come up with. And right away he saw that this gave us a reason for running the metal up over the top without shorting out the junctions, so you could actually connect this one to the next-door neighbor.”

#### Birth of Moore’s Law
The 19 April 1965 issue of *Electronics* magazine contained an article with the title “Cramming more components onto integrated circuits.” Its author, Gordon E. Moore, had been asked to predict what would happen over the next 10 years. His article speculated that by 1975 it would be possible to cram as many as 65,000 components onto a single silicon chip about 6 millimeters square.

Moore based his forecast on a log-linear plot of device complexity over time: “The complexity for minimum component costs has increased at a rate of roughly a factor of two per year. Certainly over the short term this rate can be expected to continue... Over the longer term, the rate of increase is a bit more uncertain, although there is no reason to believe it will not remain constant for at least 10 years.”

Moore revisited the subject in a paper given at the 1975 IEEE International Electron Devices Meeting. He revised his 10-year-old forecast to a doubling of component density every 18 to 24 months.

#### User Expectations Matter
Another interpretation of Moore’s Law adds to the push of new production technologies the pull of software developments. In 1995, Nathan Myhrvold, chief technology officer of Microsoft Inc., studied a variety of his firm’s products by counting the lines of code in successive releases. He observed that Microsoft Word was at 27,000 lines in its first version; the latest version had about two million lines. Myhrvold relates this to Moore’s Law:

“So we have increased the size and complexity of software even faster than Moore’s Law. In fact, this is why there is a market for faster processors—software people have always consumed new capability as fast or faster than the chip people could make it available.”

#### Is the end in view?
Moore's Law started out as a simple extrapolation from a simple observation. Actual performance and experience have validated it, proving Moore quite prophetic. Curiously, forecasts of its demise have been made throughout its existence, but have consistently been wrong.

They are still being made. A 1996 poll by Forbes of 11 industry stalwarts—chief executive officers, senior systems and software engineers, industry analysts, an industry writer, and a venture capitalist—awarded Moore's Law an average remaining life of roughly 14 years, to 2010. That would give the prediction a life span of 45 years, by any measure an incredible feat of technical insight.

At present, researchers are still learning to exploit the properties of semiconductors and production processes. But at some point this technology—like all others—will stop growing exponentially and enter the realm of diminishing marginal returns.

The physics underlying semiconductor manufacturing suggests several possible barriers to continued technical progress and density doubling. For example, the gigabit chip generation may finally force technologists up against the limits of optical lithography. There are ways around this obstacle, but the cost may be prohibitive. In fact, economics may constrain Moore's Law before physics does—an observation that others have called "Moore's second law."

For one thing, capital requirements rise exponentially along with component densities, wrote Moore in 1995. The cost of a new fabrication plant went from US $14 million in 1966 to $1500 million in 1995. By 1998, work will begin on the first $3 billion fabrication plant. These increases are no problem as long as chips improve still faster. That happened between 1984 and 1990, when chip performance tripled, while the cost of a fab plant only doubled. For the next generation of chips, however, the capital needed will double again, but performance is expected to increase only by half. If the exponential trend in fab costs continues, by 2005 the cost of a single fab plant will be more than $10 billion in 1995 dollars—more than half of Intel's current net worth.

What can semiconductor manufacturers do? One possibility: team up with customers, competitors, suppliers, or even countries to share fab construction and R&D costs. For example, the R&D price tag for dynamic RAMs, which rose from US $400 million for the 4 Mb chip to more than $1 billion for the 1 Gb device, led to the well-known alliance of IBM, Germany's Siemens, and Japan's Toshiba for the development of advanced DRAMs. In Korea and Singapore, state-organized consortia appear to be on the rise. Global alliances are emerging as the new model for competition in semiconductors.

Another economic threat to Moore's Law is the possibility that transistors will become so cheap that there will be no profit in making them cheaper. Dan Hutcheson, president of VLSI Research Inc., San Jose, Calif., reached that conclusion in 1995: "The price per transistor will bottom out sometime between 2003 and 2005. From that point on, there will be no economic point in making transistors smaller. So Moore's Law ends in seven years."

Apart from the apparent constraints of physics and economics, one respondent to the Forbes poll, Dan Lynch, president and chief executive officer of CyberCash Inc., offers a starkly different view about the future of Moore's Law, stating, "We'll be dead when Moore's Law is played out." His reasoning: "Moore's Law is about human ingenuity, not physics."

---

> **VERBATIM SOURCE**
> - **Title:** Moore's Law: The Engine of Innovation
> - **Author(s):** Intel Newsroom
> - **Published:** 2023, Intel Corporation
> - **Source type:** Corporate Technical Release
> - **Original URL:** https://download.intel.com/newsroom/2023/manufacturing/moores-law-electronics.pdf
> - **DOI:** N/A
> - **Repository:** N/A
> 
> *Note: The text below is reproduced verbatim — exact word-for-word —*
> for educational study. All rights remain with the original author(s)
> and publisher(s).

#### The Angstrom Era: RibbonFET, PowerVia, and Advanced Packaging

Intel’s roadmap emphasizes that Moore’s Law is no longer just about shrinking transistors on a single die, but about how they are designed, powered, and connected. As traditional transistor scaling faces physical limitations, Intel has shifted its strategy toward these heterogeneous and architectural innovations.

**RibbonFET:** Intel’s implementation of Gate-All-Around (GAA) transistor architecture. By wrapping the gate around the channel, RibbonFET provides superior electrostatic control compared to traditional FinFET designs, enabling faster switching speeds and improved performance per watt in a smaller footprint.

**PowerVia:** An industry-first backside power delivery technology. By moving power routing to the backside of the wafer, PowerVia separates signal and power lines. This reduces signal interference, lowers resistance, and improves power delivery efficiency, allowing for higher transistor density and performance.

**Advanced Packaging:** Techniques such as Foveros (3D stacking) and EMIB (embedded multi-die interconnect bridge), along with emerging glass substrates, are critical to Intel’s "systems foundry" approach. These technologies allow Intel to combine multiple chiplets into a single package, effectively bypassing the size constraints of individual chips and enabling the integration of up to a trillion transistors on a single package by 2030.

By combining the density benefits of RibbonFET, the efficiency gains of PowerVia, and the flexibility of advanced packaging, Intel aims to continue the trajectory of Moore’s Law—doubling transistor density and performance at a manageable cost—even as scaling individual transistors reaches the atomic level.

---

<div class="page-break"></div>

### SECTION 3: CITATION & REFERENCE DEEP-DIVES

#### Gordon E. Moore & Fairchild / Intel History
Gordon E. Moore (1929–2023) was an American engineer, businessman, and co-founder of Fairchild Semiconductor (1957) and Intel Corporation (1968). His 1965 paper in *Electronics* magazine formulated the empirical observation that component density per integrated circuit doubles roughly every year (revised in 1975 to every two years).

#### Lithography & Photolithographic Limits
Extreme Ultraviolet (EUV) lithography operates at a wavelength of 13.5 nm, replacing deep ultraviolet (DUV) immersion lithography (193 nm). As transistor features reach sub-2nm scales, quantum tunneling effects across thin gate oxides present fundamental quantum-mechanical barriers to traditional planar and FinFET scaling.

#### Chiplets & Advanced Packaging Architecture
Rather than relying on monolithic die scaling, modern high-performance processors leverage multi-chiplet disaggregation. Technologies such as TSMC's CoWoS (Chip-on-Wafer-on-Substrate) and Intel's Foveros utilize high-density silicon interposers and micro-bumps to connect disaggregated compute, memory, and I/O dies with ultra-low latency.
