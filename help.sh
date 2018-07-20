#!/bin/bash
function conversion {
	count=`ls -1 *.md 2>/dev/null | wc -l`
	if [ $count != 0 ]; then
		echo "   Found .md file. Converting..."
		rm -f *.html
		rm -f *.adoc
		for f in *.md; do
			pandoc "${f}" -f markdown_github -t html -s | sed "s/\.md/.html/g" > "${f%.md}.html"
			pandoc "${f}" -f markdown_github -t asciidoc -s | sed "s/\.md/.adoc/g" > "${f%.md}.adoc"
		done
		echo "   done"
	else
		echo "   No .md files found."
	fi
}

function recursive_search {
cd "$1"
echo $PWD
conversion
count=`ls -1 */ 2>/dev/null | wc -l`
if [ $count != 0 ]; then
	for d in */ ; do
		recursive_search "${d}"
	done
fi
cd ..
}

start=src/main/resources/help
recursive_search $start
echo "All converting done"
