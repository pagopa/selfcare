package it.pagopa.selfcare.party.registry_proxy.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.core.CategoryService;
import it.pagopa.selfcare.party.registry_proxy.web.model.CategoriesResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.CategoryResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.CategoriesMapper;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.CategoryMapper;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "category")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        log.trace("Initializing {}", CategoryController.class.getSimpleName());
        this.categoryService = categoryService;
    }

    @GetMapping("categories")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.api.category.findCategories.summary}",
            description = "${swagger.api.category.findCategories.notes}",
            operationId = "findCategoriesUsingGET")
    public CategoriesResource findCategories(@ApiParam("${swagger.model.*.origin}")
                                             @RequestParam(value = "origin", required = false) Optional<Origin> origin,
                                             @ApiParam(value = "${swagger.model.*.page}")
                                             @RequestParam(value = "page", required = false, defaultValue = "1")
                                             Integer page,
                                             @ApiParam(value = "${swagger.model.*.limit}")
                                             @RequestParam(value = "limit", required = false, defaultValue = "10")
                                             Integer limit) {
        log.trace("findCategories start");
        log.debug("findCategories origin = {}, page = {}, limit = {}", origin, page, limit);
        final QueryResult<Category> result = categoryService.search(origin, page, limit);
        final CategoriesResource categoriesResource = CategoriesMapper.toResource(result.getItems().stream()
                .map(CategoryMapper::toResource)
                .collect(Collectors.toList()));
        log.debug("findCategories result = {}", categoriesResource);
        log.trace("findCategories end");
        return categoriesResource;
    }

    @GetMapping("origins/{origin}/categories/{code}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.api.category.findCategory.summary}",
            description = "${swagger.api.category.findCategory.notes}",
            operationId = "findCategoryUsingGET")
    public CategoryResource findCategory(@ApiParam("${swagger.model.*.origin}")
                                         @PathVariable("origin") Origin origin,
                                         @ApiParam("${swagger.model.category.code}")
                                         @PathVariable("code") String code) {
        log.trace("findCategory start");
        log.debug("findCategory origin = {}, code = {}", origin, code);
        final CategoryResource categoryResource = CategoryMapper.toResource(categoryService.findById(code, origin));
        log.debug("findCategory result = {}", categoryResource);
        log.trace("findCategory end");
        return categoryResource;
    }

}
