<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:marc="http://www.loc.gov/MARC21/slim"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    exclude-result-prefixes="marc xlink">
    <xsl:output method="xml" omit-xml-declaration="no" indent="yes"/>
    
    <xsl:param name="isbn">XXXX</xsl:param>
    <xsl:param name="bibid">XXXX</xsl:param>
    
    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>
        
    <xsl:template match="marc:record">
        <xsl:variable name="nholdings"><xsl:value-of select="count(marc:datafield[@tag = '852']/marc:subfield[@code = 'b' and . != 'BHHS' and . != 'BIRB' and . != 'BOTH' and . != 'BULB' and . != 'COL' and . != 'GLBL' and . != 'IMB' and . != 'IMER' and . != 'ISDS' and . != 'ISTC' and . != 'JUDB' and . != 'KMV' and . != 'KVIN' and . != 'LOB' and . != 'MEAL' and . != 'Mmus' and . != 'NB' and . != 'NBI' and . != 'NBK' and . != 'NBM' and . != 'NBP' and . != 'NBR' and . != 'NBT' and . != 'NLT' and . != 'SAH' and . != 'SAMB' and . != 'SARB' and . != 'SB17' and . != 'SCB' and . != 'SEB' and . != 'SEE' and . != 'SEK' and . != 'SFbB' and . != 'SFIB' and . != 'SHAP' and . != 'SHB' and . != 'SHBL' and . != 'SHBS' and . != 'SHBU' and . != 'SKvB' and . != 'SLB' and . != 'SM' and . != 'SMB' and . != 'SMHB' and . != 'SOT' and . != 'SSIB' and . != 'SUEC' and . != 'Swam' and . != 'UACT' and . != 'UTR' and . != 'VIMO' and . != 'VSTM' and . !='BOKR'])"/></xsl:variable>
        <xsl:variable name="type"><xsl:call-template name="record-type"/></xsl:variable>
        
        <!--<xsl:if test="$nholdings != 0 and $type = 'book' and marc:datafield[@tag = '100' or @tag = '110' or @tag = '111' or @tag = '700' or @tag = '710' or @tag = '711']/marc:subfield[@code = 'a'] and marc:datafield[@tag = '260']/marc:subfield[@code = 'b'] and marc:datafield[@tag = '245']/marc:subfield[@code = 'a']">-->
        <xsl:if test="$nholdings != 0 and $type = 'book' and marc:datafield[@tag = '100' or @tag = '110' or @tag = '111' or @tag = '700' or @tag = '710' or @tag = '711']/marc:subfield[@code = 'a'] and marc:datafield[@tag = '245']/marc:subfield[@code = 'a']">
            <article>
                <front>
                    <!-- ISBN -->
                    <xsl:if test="$isbn != 'XXXX'">
                        <isbn><xsl:value-of select="$isbn"/></isbn>
                    </xsl:if>

                    <!-- PUBLISHER -->               
                    <!--<publisher>
                    <xsl:variable name="publisher"><xsl:value-of select="marc:datafield[@tag = '260']/marc:subfield[@code = 'b']"/></xsl:variable>
                    <xsl:choose>
                    <xsl:when test="substring($publisher, string-length($publisher), 1) = ','">
                    <publisher-name><xsl:value-of select="substring($publisher, 1, string-length($publisher)-1)"/></publisher-name>
                    </xsl:when>
                    <xsl:otherwise>
                    <publisher-name><xsl:value-of select="$publisher"/></publisher-name>
                    </xsl:otherwise>
                    </xsl:choose>-->
                    <!--<publisher-loc></publisher-loc>-->
                    <!--</publisher>-->

                    <article-meta>
                        <title-group>
                            <!--<article-title><xsl:value-of select="normalize-space(translate(marc:datafield[@tag = '245']/marc:subfield[@code = 'a'], ':/', ''))"/></article-title>-->
                            <xsl:variable name="title"><xsl:for-each select="marc:datafield[@tag = '245']/marc:subfield[@code != 'c']"><xsl:value-of select="concat(translate(., '/', ''), ' ')"/></xsl:for-each></xsl:variable>
                            <article-title><xsl:value-of select="normalize-space($title)"/></article-title>
                            <!--<trans-title></trans-title>-->
                        </title-group>

                        <contrib-group>
                            <xsl:for-each select="marc:datafield[@tag = '100' or @tag = '110' or @tag = '111' or @tag = '700' or @tag = '710' or @tag = '711']">
                                <xsl:variable name="author_type"><xsl:call-template name="author-type"/></xsl:variable>
                                <xsl:variable name="author"><xsl:value-of select="marc:subfield[@code = 'a']"/></xsl:variable>
                                <contrib contrib-type="{$author_type}">
                                    <name>
                                        <xsl:if test="$author_type='Author'">
                                            <surname><xsl:value-of select="substring-before($author, ',')"/></surname>
                                            <given-names><xsl:value-of select="normalize-space(translate(substring-after($author, ','), ',', ''))"/></given-names>
                                        </xsl:if>
                                        <xsl:if test="$author_type='Corporation'">
                                            <surname><xsl:value-of select="$author"/></surname>
                                        </xsl:if>
                                        <xsl:if test="marc:subfield[@code = 'b']">
                                            <suffix><xsl:value-of select="marc:subfield[@code = 'b']"/></suffix>
                                        </xsl:if>
                                    </name>
                                </contrib>
                            </xsl:for-each>
                        </contrib-group>

                        <xsl:if test="marc:datafield[@tag = '976']">
                            <kwd-group>
                                <xsl:for-each select="marc:datafield[@tag = '976']/marc:subfield[@code = 'b']">
                                    <kwd><xsl:value-of select="."/></kwd>
                                </xsl:for-each>
                            </kwd-group>
                        </xsl:if>

                        <xsl:if test="marc:datafield[@tag = '650' or @tag = '653']/marc:subfield[@code = 'a']">
                            <article-categories>
                                <subj-group>
                                    <xsl:for-each select="marc:datafield[@tag = '650' or @tag = '653']">
                                        <subject><xsl:value-of select="marc:subfield[@code = 'a']"/></subject>
                                    </xsl:for-each>
                                </subj-group>
                            </article-categories>
                        </xsl:if>

                        <num_libraries><xsl:value-of select="$nholdings"/></num_libraries>                    
                        <self-uri xlink:href="http://websok.libris.kb.se/websearch/googlelink?bibid={$bibid}"/>
                    </article-meta>
                </front>
                <article-type>book</article-type>
            </article>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="author-type">
        <xsl:variable name="type" select="@tag"/>
        <xsl:if test="$type='100' or $type='700'">Author</xsl:if>
        <xsl:if test="$type='110' or $type='710'">Corporation</xsl:if>
        <xsl:if test="$type='111' or $type='711'">Corporation</xsl:if>
    </xsl:template>
    
    <xsl:template name="record-type">
        <xsl:variable name="leader_6"><xsl:value-of select="substring(marc:leader, 7, 1)"/></xsl:variable>
        <xsl:variable name="leader_7"><xsl:value-of select="substring(marc:leader, 8, 1)"/></xsl:variable>
        <xsl:variable name="cf007_0"><xsl:value-of select="substring(marc:controlfield[@tag = '007'], 1, 1)"/></xsl:variable>
        <xsl:variable name="cf007_1"><xsl:value-of select="substring(marc:controlfield[@tag = '007'], 2, 1)"/></xsl:variable>
        <xsl:variable name="cf007_0-1"><xsl:value-of select="substring(marc:controlfield[@tag = '007'], 1, 2)"/></xsl:variable>
        <xsl:variable name="cf008_21"><xsl:value-of select="substring(marc:controlfield[@tag = '008'], 22, 1)"/></xsl:variable>
        <xsl:variable name="cf008_24"><xsl:value-of select="substring(marc:controlfield[@tag = '008'], 25, 1)"/></xsl:variable>
        <xsl:choose>
            <xsl:when test="marc:datafield[@tag = '245']/marc:subfield[@code = 'h'] = '[Affisch]'">poster</xsl:when>
            <xsl:when test="$leader_6 = 'a' and ($leader_7 = 'a' or $leader_7 = 'b')">article</xsl:when>
            <xsl:when test="$leader_6 = 'k'">picture</xsl:when>
            <xsl:when test="$leader_6 = 'k' and $leader_7 = 'm'">book</xsl:when>
            <xsl:when test="($leader_6 = 'i' or leader_6 = 's') and $cf008_21 = 'n'">newspaper</xsl:when>
            <xsl:when test="($leader_6 = 'i' or leader_6 = 's') and $cf008_21 = 'd'">database</xsl:when>
            <xsl:when test="$leader_6 = 'm'">digital-medium</xsl:when>
            <xsl:when test="($leader_6 = 'a' or $leader_6 = 't') and ($leader_7 = 'a' or $leader_7 = 'b') and $cf007_0-1 = 'cr'">e-article</xsl:when>
            <xsl:when test="$leader_6 = 'k' and $cf007_0-1 = 'cr'">e-picture</xsl:when>
            <xsl:when test="($leader_6 = 'a' or $leader_6 = 't') and $leader_7 = 'm' and $cf007_0-1 = 'cr'">e-book</xsl:when>
            <xsl:when test="($leader_6 = 'e' or $leader_6 = 'f') and $cf007_0-1 = 'cr'">e-map</xsl:when>
            <xsl:when test="$cf007_0-1 = 'cr'">e-resource</xsl:when>
            <xsl:when test="($leader_6 = 'a' or $leader_6 = 't') and ($leader_7 = 'i' or $leader_7 = 's') and $cf007_0-1 = 'cr'">e-journal</xsl:when>
            <xsl:when test="$leader_6 = 'g'">film-video</xsl:when>
            <xsl:when test="$leader_6 = 'p'">mixed-media</xsl:when>
            <xsl:when test="$leader_6 = 'r'">realia</xsl:when>
            <xsl:when test="$leader_6 = 't'">manuscript</xsl:when>
            <xsl:when test="($leader_6 = 'e' or $leader_6 = 'f')">map</xsl:when>
            <xsl:when test="$leader_6 = 'i'">sound</xsl:when>
            <xsl:when test="$leader_6 = 'o'">kit</xsl:when>
            <xsl:when test="($leader_7 = 'i' or $leader_7 = 's') and $cf008_21 = 'l'">looseleaf</xsl:when>
            <xsl:when test="$cf007_0 = 'h'">microform</xsl:when>
            <xsl:when test="$leader_6 = 'j'">music</xsl:when>
            <xsl:when test="$leader_6 = 'c' or $leader_6 = 'd'">notated-music</xsl:when>
            <xsl:when test="$leader_6 = 'a' and ($leader_7 = 'a' or $leader_7 = 'b') and $cf008_24 = 'o'">review</xsl:when>
            <xsl:when test="$leader_7 = 'c' or $leader_7 = 'd'">collection</xsl:when>
            <xsl:when test="($leader_7 = 'i' or $leader_7 = 's') and $cf008_21 = 'm'">monographic-series</xsl:when>
            <xsl:when test="$leader_7 = 'i' or $leader_7 = 's'">serial</xsl:when>
            <xsl:when test="$cf007_0 = 'f'">tactile</xsl:when>
            <xsl:when test="$leader_6 = 'a' and ($leader_7 = 'i' or $leader_7 = 's') and $cf008_21 = 'p'">journal</xsl:when>
            <xsl:when test="($leader_7 = 'i' or $leader_7 = 's') and $cf008_21 = 'w'">website</xsl:when>
            <xsl:when test="$cf007_0 = 'c'">digital-medium</xsl:when>
            <xsl:otherwise>book</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
