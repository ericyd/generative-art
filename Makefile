fmtkt:
	./gradlew ktlintFormat

fmtrs:
	cargo fmt

# Not sure why, but the new IntelliJ doesn't kill existing OPENRNDR sketches when you rebuild, so they stack up and burn the CPU
killjava:
	 ps -ax | rg -v 'rg java' | rg java | rg -o "^\s(\d+)\s" | rg -o '\d+' | xargs kill

.PHONY: fmtkt, fmtrs, killjava
