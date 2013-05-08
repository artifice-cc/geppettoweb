library(reshape2)

m <- melt(control, id.vars=c("simulation",
                     <(if (:facethoriz graph) (format "\"%s\"," (:facethoriz graph)) "")>
                     <(if (:facetvert graph) (format "\"%s\"," (:facetvert graph)) "")>
                     <(if (:color graph) (format "\"%s\"," (:color graph)) "")>
                     <(if (:shape graph) (format "\"%s\"," (:shape graph)) "")>
                     <(format "\"%s\"" (:xfield graph))>
                     ),
          measure.vars=c(<(format "\"%s\"" (:yfield graph))>))

p <- ggplot(m, aes(x=<(:xfield graph)>, y=value,
                   <(if (:color graph) (format "color=factor(%s)," (:color graph)) "")>
                   <(if (:shape graph) (format "shape=factor(%s)," (:shape graph)) "")>
                   ))
p <- p + geom_point()

p <- p + scale_x_continuous("<(if (not= "" (:xlabel graph)) (:xlabel graph) (:xfield graph))>")
p <- p + scale_y_continuous("<(if (not= "" (:ylabel graph)) (:ylabel graph) (:yfield graph))>")

p <- p + labs(
           <(if (:color graph) (format "color=\"%s\"," (:color graph)) "")>
           <(if (:shape graph) (format "shape=\"%s\"," (:shape graph)) "")>
           dummy="")

<(if (or (:facethoriz graph) (:facetvert graph)) ">
p <- p + facet_grid(
<(str (if (:facetvert graph) (:facetvert graph) "."))>
~
<(str (if (:facethoriz graph) (:facethoriz graph) "."))>
)
<")>
