/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST CMS Components.
 *
 * FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST CMS Components is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.cmscomponents.ui.spring;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.fenixedu.bennu.io.servlets.FileDownloadServlet.getDownloadUrl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.exceptions.BennuCoreDomainException;
import org.fenixedu.bennu.core.groups.AnyoneGroup;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.cms.domain.CMSTemplate;
import org.fenixedu.cms.domain.Category;
import org.fenixedu.cms.domain.Post;
import org.fenixedu.cms.domain.PostMetadata;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.exceptions.CmsDomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import pt.ist.fenixedu.cmscomponents.domain.unit.UnitSite;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Created by borgez on 18-03-2015.
 */
@SpringApplication(group = "logged", path = "unit-sites", title = "unit.site.management.title")
@SpringFunctionality(accessGroup = "logged", app = UnitSiteManagementController.class, title = "unit.site.management.title")
@RequestMapping("/unit/sites")
public class UnitSiteManagementController {

    private static final int ITEMS_PER_PAGE = 30;

    @RequestMapping
    public String list(Model model) {
        return list(0, model);
    }

    @RequestMapping(value = "manage/{page}", method = RequestMethod.GET)
    public String list(@PathVariable(value = "page") int page, Model model) {
        List<List<Site>> pages = Lists.partition(getSites(), ITEMS_PER_PAGE);
        int currentPage = normalize(page, pages);
        model.addAttribute("numberOfPages", pages.size());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("sites", pages.isEmpty() ? Collections.emptyList() : pages.get(currentPage));
        model.addAttribute("isManager", DynamicGroup.get("managers").isMember(Authenticate.getUser()));
        return "fenix-learning/istSites";
    }

    private int normalize(int page, List<List<Site>> pages) {
        if (page < 0) {
            return 0;
        }
        if (page >= pages.size()) {
            return pages.size() - 1;
        }
        return page;
    }

    private List<Site> getSites() {
        User user = Authenticate.getUser();
        Set<Site> allSites = Bennu.getInstance().getSitesSet();
        Predicate<Site> isAdminMember = site -> site.getCanAdminGroup().isMember(user);
        Predicate<Site> isPostsMember = site -> site.getCanPostGroup().isMember(user);
        return allSites.stream().filter(isAdminMember.or(isPostsMember)).collect(Collectors.toList());
    }

    @RequestMapping(value = "/{unitSiteSlug}")
    public String manageSite(Model model, @PathVariable String unitSiteSlug) {
        Site unitSite = site(unitSiteSlug);
        model.addAttribute("banners", getBanners(unitSite));
        model.addAttribute("unitSite", unitSite);
        return "fenix-learning/unitSiteManagement";
    }

    private List<BannerBean> getBanners(Site unitSite) {
        Category category = unitSite.categoryForSlug("banner");
        if (category != null) {
            return category.getPostsSet().stream().filter(post -> post.getMetadata() != null).map(BannerBean::new)
                    .collect(toList());
        }
        return ImmutableList.of();
    }

    @RequestMapping(value = "/{unitSiteSlug}/layout", method = RequestMethod.POST)
    public RedirectView editLayout(@PathVariable String unitSiteSlug, @RequestParam String template) {
        Site unitSite = site(unitSiteSlug);
        CMSTemplate cmsTemplate = unitSite.getTheme().templateForType(template);
        if (cmsTemplate != null && !cmsTemplate.equals(unitSite.getInitialPage().getTemplate())) {
            FenixFramework.atomic(() -> {
                unitSite.getInitialPage().setTemplate(cmsTemplate);
            });
        }
        return new RedirectView("/unit/sites", true);
    }

    @RequestMapping(value = "/{unitSiteSlug}/create", method = RequestMethod.POST)
    public RedirectView createBanner(@PathVariable String unitSiteSlug, BannerBean banner) {
        Site unitSite = site(unitSiteSlug);
        FenixFramework.atomic(() -> {
            banner.setPost(new Post(unitSite));
            banner.save();
        });
        return defaultRedirect(unitSite);
    }

    @RequestMapping(value = "/{unitSiteSlug}/{postSlug}/update", method = RequestMethod.POST)
    public RedirectView updateBanner(@PathVariable String unitSiteSlug, @PathVariable String postSlug, BannerBean banner) {
        Site unitSite = site(unitSiteSlug);
        FenixFramework.atomic(() -> {
            banner.setPost(unitSite.postForSlug(postSlug));
            banner.save();
        });
        return defaultRedirect(unitSite);
    }

    @RequestMapping(value = "/{unitSiteSlug}/{postSlug}/delete", method = RequestMethod.POST)
    public RedirectView deleteBanner(@PathVariable String unitSiteSlug, @PathVariable String postSlug) {
        Site unitSite = site(unitSiteSlug);
        FenixFramework.atomic(() -> {
            Post post = unitSite.postForSlug(postSlug);
            if (FenixFramework.isDomainObjectValid(post)) {
                post.delete();
            }
        });
        return defaultRedirect(unitSite);
    }

    @RequestMapping(value = "default", method = RequestMethod.POST)
    public RedirectView setAsDefault(@RequestParam String slug) {
        Site s = Site.fromSlug(slug);

        if (!DynamicGroup.get("managers").isMember(Authenticate.getUser())) {
            throw CmsDomainException.forbiden();
        }

        makeDefaultSite(s);

        return new RedirectView("/unit/sites", true);
    }

    @Atomic
    private void makeDefaultSite(Site s) {
        Bennu.getInstance().setDefaultSite(s);
    }

    private RedirectView defaultRedirect(Site unitSite) {
        return new RedirectView(String.format("/unit/sites/%s", unitSite.getSlug()), true);
    }

    private Site site(String unitSiteSlug) {
        Site site = Site.fromSlug(unitSiteSlug);
        if (!FenixFramework.isDomainObjectValid(site)) {
            throw BennuCoreDomainException.resourceNotFound(unitSiteSlug);
        }
        if (site instanceof UnitSite) {
            if (!site.getCanAdminGroup().isMember(Authenticate.getUser())) {
                throw CmsDomainException.forbiden();
            }
        }
        return site;
    }

    public static class BannerBean {
        private LocalizedString name;
        private Boolean showIntroduction;
        private Boolean showAnnouncements;
        private Boolean showEvents;
        private Boolean showBanner;
        private String color;
        private MultipartFile mainImage;
        private String mainImageUrl;
        private Post post;
        private String bannerUrl;

        public BannerBean() {
        }

        public BannerBean(Post post) {
            this.post = post;
            this.name = post.getName();
            this.showIntroduction = post.getMetadata().getAsBoolean("showIntroduction").orElse(true);
            this.showAnnouncements = post.getMetadata().getAsBoolean("showAnnouncements").orElse(true);
            this.showEvents = post.getMetadata().getAsBoolean("showEvents").orElse(true);
            this.showBanner = post.getMetadata().getAsBoolean("showBanner").orElse(true);
            this.color = post.getMetadata().getAsString("color").orElse("white");
            this.mainImageUrl = post.getMetadata().getAsString("mainImage").orElse(null);
            this.bannerUrl = post.getMetadata().getAsString("link").orElse(null);
        }

        public void save() {
            PostMetadata postMetadata = ofNullable(post.getMetadata()).orElseGet(PostMetadata::new);
            Category bannerCategory = getOrCreateBannerCategory(post.getSite());

            if (!post.getCategoriesSet().contains(bannerCategory)) {
                post.addCategories(bannerCategory);
            }

            if (name != null) {
                post.setName(name);
            }

            postMetadata = postMetadata.with("showIntroduction", ofNullable(showIntroduction).orElse(false));
            postMetadata = postMetadata.with("showAnnouncements", ofNullable(showAnnouncements).orElse(false));
            postMetadata = postMetadata.with("showEvents", ofNullable(showEvents).orElse(false));
            postMetadata = postMetadata.with("showBanner", ofNullable(showBanner).orElse(false));
            postMetadata = postMetadata.with("color", ofNullable(color).orElse("#ffffff"));
            postMetadata = uploadImage(post, postMetadata, "mainImage", mainImage);
            postMetadata = postMetadata.with("link", ofNullable(bannerUrl).orElse("#"));

            post.setMetadata(postMetadata);
        }

        private PostMetadata uploadImage(Post post, PostMetadata postMetadata, String name, MultipartFile multipartFile) {
            if (!Strings.isNullOrEmpty(name) && multipartFile != null && !multipartFile.isEmpty()) {
                try {
                    GroupBasedFile file =
                            new GroupBasedFile(multipartFile.getOriginalFilename(), multipartFile.getOriginalFilename(),
                                    multipartFile.getBytes(), AnyoneGroup.get());
                    post.getPostFiles().putFile(file);
                    postMetadata = postMetadata.with(name, getDownloadUrl(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return postMetadata;
        }

        private Category getOrCreateBannerCategory(Site site) {
            return site.getOrCreateCategoryForSlug("banner", new LocalizedString(Locale.getDefault(), "Banner"));
        }

        /*
        * getters and setters
        * */

        public Post getPost() {
            return this.post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public LocalizedString getName() {
            return name;
        }

        public void setName(LocalizedString name) {
            this.name = name;
        }

        public Boolean getShowIntroduction() {
            return showIntroduction;
        }

        public void setShowIntroduction(Boolean showIntroduction) {
            this.showIntroduction = showIntroduction;
        }

        public Boolean getShowAnnouncements() {
            return showAnnouncements;
        }

        public void setShowAnnouncements(Boolean showAnnouncements) {
            this.showAnnouncements = showAnnouncements;
        }

        public Boolean getShowEvents() {
            return showEvents;
        }

        public void setShowEvents(Boolean showEvents) {
            this.showEvents = showEvents;
        }

        public Boolean getShowBanner() {
            return showBanner;
        }

        public void setShowBanner(Boolean showBanner) {
            this.showBanner = showBanner;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public MultipartFile getMainImage() {
            return mainImage;
        }

        public void setMainImage(MultipartFile mainImage) {
            this.mainImage = mainImage;
        }

        public String getMainImageUrl() {
            return mainImageUrl;
        }

        public void setMainImageUrl(String mainImageUrl) {
            this.mainImageUrl = mainImageUrl;
        }

        public String getBannerUrl() {
            return bannerUrl;
        }

        public void setBannerUrl(String bannerUrl) {
            this.bannerUrl = bannerUrl;
        }

    }
}
