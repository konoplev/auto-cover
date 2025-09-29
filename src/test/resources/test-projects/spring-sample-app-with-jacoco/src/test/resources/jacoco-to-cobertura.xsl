<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" indent="yes"/>

  <xsl:template match="/report">
    <coverage line-rate="0" branch-rate="0" version="1.9" timestamp="{/@sessioninfo[@id][1]/@start}">
      <sources>
        <xsl:for-each select="package/sourcefile[not(@name=preceding::sourcefile/@name)]">
          <source><xsl:value-of select="@name"/></source>
        </xsl:for-each>
      </sources>
      <packages>
        <xsl:for-each select="package">
          <package name="{ @name }" line-rate="0" branch-rate="0" complexity="0">
            <classes>
              <xsl:for-each select="class">
                <class name="{ @name }" filename="{ translate(@sourcefilename, '\\', '/') }" line-rate="0" branch-rate="0" complexity="0">
                  <methods>
                    <xsl:for-each select="method">
                      <method name="{ @name }" signature="{ @desc }" line-rate="0" branch-rate="0">
                        <lines>
                          <xsl:for-each select="line">
                            <line number="{ @nr }" hits="{ @ci }">
                              <xsl:if test="@mb or @cb">
                                <xsl:attribute name="branch">true</xsl:attribute>
                                <xsl:attribute name="condition-coverage">
                                  <xsl:value-of select="concat(round(100 * @cb div @mb), '% (', @cb, '/', @mb, ')')"/>
                                </xsl:attribute>
                              </xsl:if>
                            </line>
                          </xsl:for-each>
                        </lines>
                      </method>
                    </xsl:for-each>
                  </methods>
                  <lines>
                    <xsl:for-each select="method/line">
                      <line number="{ @nr }" hits="{ @ci }">
                        <xsl:if test="@mb or @cb">
                          <xsl:attribute name="branch">true</xsl:attribute>
                          <xsl:attribute name="condition-coverage">
                            <xsl:value-of select="concat(round(100 * @cb div @mb), '% (', @cb, '/', @mb, ')')"/>
                          </xsl:attribute>
                        </xsl:if>
                      </line>
                    </xsl:for-each>
                  </lines>
                </class>
              </xsl:for-each>
            </classes>
          </package>
        </xsl:for-each>
      </packages>
    </coverage>
  </xsl:template>

  <!-- Identity transform fallback for any nodes not explicitly handled -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>


