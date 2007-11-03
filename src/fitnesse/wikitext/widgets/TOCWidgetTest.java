// Copyright (C) 2003,2004,2005 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the GNU General Public License version 2 or later.
package fitnesse.wikitext.widgets;

import fitnesse.html.HtmlElement;
import fitnesse.testutil.FitNesseUtil;
import fitnesse.wiki.*;

public class TOCWidgetTest extends WidgetTestCase
{
	private WikiPage root;
	private WikiPage parent, parent2;
	private PageCrawler crawler;
	private String endl = HtmlElement.endl;

	public void setUp() throws Exception
	{
		root = InMemoryPage.makeRoot("RooT");
		crawler = root.getPageCrawler();
		parent = crawler.addPage(root, PathParser.parse("ParenT"), "parent");
		crawler.addPage(root, PathParser.parse("ParentTwo"), "parent two");

      crawler.addPage(parent, PathParser.parse("ChildOne"), "content");
		crawler.addPage(parent, PathParser.parse("ChildTwo"), "content");
		//[acd] Regracing
      parent2 = crawler.addPage(root, PathParser.parse("ParenT2"), "parent2");
      crawler.addPage(parent2, PathParser.parse("Child1Page"), "content");
      crawler.addPage(parent2, PathParser.parse("Child2Page"), "content");
	}

	public void tearDown() throws Exception
	{
	}

	public void testMatch() throws Exception
	{
		assertMatchEquals("!contents\n", "!contents");
		assertMatchEquals("!contents -R\n", "!contents -R");
		assertMatchEquals("!contents\r", "!contents");
		assertMatchEquals("!contents -R\r", "!contents -R");
		assertMatchEquals(" !contents\n", null);
		assertMatchEquals(" !contents -R\n", null);
		assertMatchEquals("!contents zap\n", null);
		assertMatchEquals("!contents \n", "!contents ");
      //[acd] !contents: -R[0-9]...
      assertMatchEquals("!contents -R0\n", "!contents -R0");
      assertMatchEquals("!contents -R1\n", "!contents -R1");
      assertMatchEquals("!contents -R99\n", "!contents -R99");
      assertMatchEquals("!contents -Rx\n", null);
      
      //[acd] Regracing
      assertMatchEquals("!contents -g\n", "!contents -g");
      assertMatchEquals("!contents -R -g\n", "!contents -R -g");
      assertMatchEquals("!contents -g\r", "!contents -g");
      assertMatchEquals("!contents -R -g\r", "!contents -R -g");
      assertMatchEquals(" !contents    -g\n", null);
      assertMatchEquals(" !contents -R -g\n", null);
      assertMatchEquals("!contents -gx\n", null);
      assertMatchEquals("!contents -g \n", "!contents -g ");
	}

	public void testNoGrandchildren() throws Exception
	{
		assertEquals(getHtmlWithNoHierarchy(), renderNormalTOCWidget());
		assertEquals(getHtmlWithNoHierarchy(), renderHierarchicalTOCWidget());
	}

	public void testWithGrandchildren() throws Exception
	{
		addGrandChild(parent, "ChildOne");
		assertEquals(getHtmlWithNoHierarchy(), renderNormalTOCWidget());
		assertEquals(getHtmlWithGrandChild(), renderHierarchicalTOCWidget());
	}

	public void testWithGreatGrandchildren() throws Exception
	{
		addGrandChild(parent, "ChildOne");
		addGreatGrandChild(parent, "ChildOne");
		assertEquals(getHtmlWithNoHierarchy(), renderNormalTOCWidget());
		assertEquals(getHtmlWithGreatGrandChild(), renderHierarchicalTOCWidget());
	}
   
   public void testWithGreatGrandchildrenRegraced() throws Exception  //[acd] Regracing
   {
      addGrandChild(parent2, "Child1Page");
      addGreatGrandChild(parent2, "Child1Page");
      assertEquals(getHtmlWithNoHierarchyRegraced(), renderNormalRegracedTOCWidget());
      assertEquals(getHtmlWithGreatGrandChildRegraced(), renderHierarchicalRegracedTOCWidget());
   }

	public void testTocOnRoot() throws Exception
	{
		TOCWidget widget = new TOCWidget(new WidgetRoot(root), "!contents\n");
		String html = widget.render();
		assertHasRegexp("ParenT", html);
		assertHasRegexp("ParentTwo", html);
	}

	public void testDisplaysVirtualChildren() throws Exception
	{
		WikiPage page = crawler.addPage(root, PathParser.parse("VirtualParent"));
		PageData data = page.getData();
		data.setAttribute(WikiPageProperties.VIRTUAL_WIKI_ATTRIBUTE, "http://localhost:" + FitNesseUtil.port + "/ParenT");
		page.commit(data);
		try
		{
			FitNesseUtil.startFitnesse(root);
			TOCWidget widget = new TOCWidget(new WidgetRoot(page), "!contents\n");
			String html = widget.render();
			assertEquals(virtualChildrenHtml(), html);
		}
		finally
		{
			FitNesseUtil.stopFitnesse();
		}
	}
   
	public void testIsNotHierarchical() throws Exception
	{
		assertFalse(new TOCWidget(new WidgetRoot(parent), "!contents\n").isRecursive());
	}

	public void testIsHierarchical() throws Exception
	{
		assertTrue(new TOCWidget(new WidgetRoot(parent), "!contents -R\n").isRecursive());
	}

	private void addGrandChild(WikiPage parent, String childName)
	throws Exception
	{
	   crawler.addPage(parent.getChildPage(childName), PathParser.parse("GrandChild"), "content");
	}

	private void addGreatGrandChild(WikiPage parent, String childName)
	throws Exception
	{
	   crawler.addPage(parent.getChildPage(childName).getChildPage("GrandChild"), PathParser.parse("GreatGrandChild"), "content");
	}

	private String renderNormalTOCWidget()
		throws Exception
	{
		return new TOCWidget(new WidgetRoot(parent), "!contents\n").render();
	}

	private String renderHierarchicalTOCWidget()
		throws Exception
	{
		return new TOCWidget(new WidgetRoot(parent), "!contents -R\n").render();
	}

	private String renderNormalRegracedTOCWidget()
	   throws Exception
	{
      WidgetRoot root = new WidgetRoot(parent2);
      root.addVariable(TOCWidget.REGRACE_TOC, "true");
	   return new TOCWidget(root, "!contents\n").render();
	}

	private String renderHierarchicalRegracedTOCWidget()
	   throws Exception
	{
      WidgetRoot root = new WidgetRoot(parent2);
      root.addVariable(TOCWidget.REGRACE_TOC, "true");
	   return new TOCWidget(root, "!contents -R\n").render();
	}

	private String getHtmlWithNoHierarchy()
	{
		return
			"<div class=\"toc1\">" + endl +
				"\t<ul>" + endl +
				"\t\t<li>" + endl +
				"\t\t\t<a href=\"ParenT.ChildOne\">ChildOne</a>" + endl +
				"\t\t</li>" + endl +
				"\t\t<li>" + endl +
				"\t\t\t<a href=\"ParenT.ChildTwo\">ChildTwo</a>" + endl +
				"\t\t</li>" + endl +
				"\t</ul>" + endl +
				"</div>" + endl;
	}

	private String getHtmlWithGrandChild()
	{
		return
			"<div class=\"toc1\">" + endl +
				"\t<ul>" + endl +
				"\t\t<li>" + endl +
				"\t\t\t<a href=\"ParenT.ChildOne\">ChildOne</a>" + endl +
				"\t\t\t<div class=\"toc2\">" + endl +
				"\t\t\t\t<ul>" + endl +
				"\t\t\t\t\t<li>" + endl +
				"\t\t\t\t\t\t<a href=\"ParenT.ChildOne.GrandChild\">GrandChild</a>" + endl +
				"\t\t\t\t\t</li>" + endl +
				"\t\t\t\t</ul>" + endl +
				"\t\t\t</div>" + endl +
				"\t\t</li>" + endl +
				"\t\t<li>" + endl +
				"\t\t\t<a href=\"ParenT.ChildTwo\">ChildTwo</a>" + endl +
				"\t\t</li>" + endl +
				"\t</ul>" + endl +
				"</div>" + endl;
	}

	private String getHtmlWithGreatGrandChild()
	{
		String expected =
			"<div class=\"toc1\">" + endl +
				"\t<ul>" + endl +
				"\t\t<li>" + endl +
				"\t\t\t<a href=\"ParenT.ChildOne\">ChildOne</a>" + endl +
				"\t\t\t<div class=\"toc2\">" + endl +
				"\t\t\t\t<ul>" + endl +
				"\t\t\t\t\t<li>" + endl +
				"\t\t\t\t\t\t<a href=\"ParenT.ChildOne.GrandChild\">GrandChild</a>" + endl +
				"\t\t\t\t\t\t<div class=\"toc3\">" + endl +
				"\t\t\t\t\t\t\t<ul>" + endl +
				"\t\t\t\t\t\t\t\t<li>" + endl +
				"\t\t\t\t\t\t\t\t\t<a href=\"ParenT.ChildOne.GrandChild.GreatGrandChild\">GreatGrandChild</a>" + endl +
				"\t\t\t\t\t\t\t\t</li>" + endl +
				"\t\t\t\t\t\t\t</ul>" + endl +
				"\t\t\t\t\t\t</div>" + endl +
				"\t\t\t\t\t</li>" + endl +
				"\t\t\t\t</ul>" + endl +
				"\t\t\t</div>" + endl +
				"\t\t</li>" + endl +
				"\t\t<li>" + endl +
				"\t\t\t<a href=\"ParenT.ChildTwo\">ChildTwo</a>" + endl +
				"\t\t</li>" + endl +
				"\t</ul>" + endl +
				"</div>" + endl;
		return expected;
	}

   private String getHtmlWithNoHierarchyRegraced()
   {
      return
         "<div class=\"toc1\">" + endl +
            "\t<ul>" + endl +
            "\t\t<li>" + endl +
            "\t\t\t<a href=\"ParenT2.Child1Page\">Child 1 Page</a>" + endl +
            "\t\t</li>" + endl +
            "\t\t<li>" + endl +
            "\t\t\t<a href=\"ParenT2.Child2Page\">Child 2 Page</a>" + endl +
            "\t\t</li>" + endl +
            "\t</ul>" + endl +
            "</div>" + endl;
   }

   private String getHtmlWithGreatGrandChildRegraced()  //[acd] Regracing
   {
      String expected =
         "<div class=\"toc1\">" + endl +
            "\t<ul>" + endl +
            "\t\t<li>" + endl +
            "\t\t\t<a href=\"ParenT2.Child1Page\">Child 1 Page</a>" + endl +
            "\t\t\t<div class=\"toc2\">" + endl +
            "\t\t\t\t<ul>" + endl +
            "\t\t\t\t\t<li>" + endl +
            "\t\t\t\t\t\t<a href=\"ParenT2.Child1Page.GrandChild\">Grand Child</a>" + endl +
            "\t\t\t\t\t\t<div class=\"toc3\">" + endl +
            "\t\t\t\t\t\t\t<ul>" + endl +
            "\t\t\t\t\t\t\t\t<li>" + endl +
            "\t\t\t\t\t\t\t\t\t<a href=\"ParenT2.Child1Page.GrandChild.GreatGrandChild\">Great Grand Child</a>" + endl +
            "\t\t\t\t\t\t\t\t</li>" + endl +
            "\t\t\t\t\t\t\t</ul>" + endl +
            "\t\t\t\t\t\t</div>" + endl +
            "\t\t\t\t\t</li>" + endl +
            "\t\t\t\t</ul>" + endl +
            "\t\t\t</div>" + endl +
            "\t\t</li>" + endl +
            "\t\t<li>" + endl +
            "\t\t\t<a href=\"ParenT2.Child2Page\">Child 2 Page</a>" + endl +
            "\t\t</li>" + endl +
            "\t</ul>" + endl +
            "</div>" + endl;
      return expected;
   }


	private String virtualChildrenHtml()
	{
		return "<div class=\"toc1\">" + endl +
			"\t<ul>" + endl +
			"\t\t<li>" + endl +
			"\t\t\t<a href=\"VirtualParent.ChildOne\">" + endl +
			"\t\t\t\t<i>ChildOne</i>" + endl +
			"\t\t\t</a>" + endl +
			"\t\t</li>" + endl +
			"\t\t<li>" + endl +
			"\t\t\t<a href=\"VirtualParent.ChildTwo\">" + endl +
			"\t\t\t\t<i>ChildTwo</i>" + endl +
			"\t\t\t</a>" + endl +
			"\t\t</li>" + endl +
			"\t</ul>" + endl +
			"</div>" + endl;

	}

	protected String getRegexp()
	{
		return TOCWidget.REGEXP;
	}
}
