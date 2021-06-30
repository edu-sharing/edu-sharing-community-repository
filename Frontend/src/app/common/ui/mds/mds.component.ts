import {
    Component,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    NgZone,
    Output,
    ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Translation } from '../../../core-ui-module/translation';
import { ActivatedRoute } from '@angular/router';
import { Toast } from '../../../core-ui-module/toast';
import { VCard } from '../../../core-module/ui/VCard';
import { Helper } from '../../../core-module/rest/helper';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import {NodeHelperService} from '../../../core-ui-module/node-helper.service';
import { trigger } from '@angular/animations';
import { UIAnimation } from '../../../core-module/ui/ui-animation';
import {
    ConfigurationService,
    Node,
    NodeList,
    DialogButton,
    MdsValueList,
    NodeWrapper,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestLocatorService,
    RestMdsService,
    RestNodeService,
    RestSearchService,
    RestToolService,
    SessionStorageService,
    UIService,
    RestUtilitiesService,
} from '../../../core-module/core.module';
import { CardJumpmark } from '../../../core-ui-module/components/card/card.component';
import { MdsHelper } from '../../../core-module/rest/mds-helper';
import { Observable } from 'rxjs';
import { MdsType, UserPresentableError, MdsDefinition } from '../mds-editor/types';
import { MdsEditorCommonService } from '../mds-editor/mds-editor-common.service';
import {DateHelper} from '../../../core-ui-module/DateHelper';
declare var noUiSlider: any;

@Component({
    selector: 'mds',
    templateUrl: 'mds.component.html',
    styleUrls: ['mds.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class MdsComponent {
    /**
     * priority, useful if the dialog seems not to be in the foreground
     * Values greater 0 will raise the z-index
     * Default is 1 for mds
     */
    @Input() priority = 1;
    @Input() addWidget = false;
    @Input() embedded = false;

    /**
     * bulk behaviour: this controls how the bulk feature shall behave
     */
    @Input() bulkBehaviour = BulkBehavior.Default;

    private activeAuthorType: number;
    private static VCARD_FIELDS=["Title","Givenname","Surname"];
    /**
     * Can the node content be replaced?
     */
    @Input() allowReplacing = true;
    @Input() parentNode: Node;

    /**
     * custom label for save (positive) action
     */
    @Input() labelPositive = 'SAVE';
    /**
     * custom label for cancel (negative) action
     */
    @Input() labelNegative = 'CANCEL';
    /**
     * custom title for card (only works if you don't edit an actual node)
     */
    @Input() customTitle: string;
    private _setId = RestConstants.DEFAULT;
    private _suggestions: any;
    private _groupId: string;
    private _repository = RestConstants.HOME_REPOSITORY;
    private createType: string;
    private _currentValues: any;
    /**
     * Show extended widgets
     */
    @Input() extended = false;
    /**
     * mode, currently "search" or "default"
     * @type {string}
     */
  @Input() mode : 'search' | 'default'='default';

    @Output() extendedChange = new EventEmitter();
    private static AUTHOR_TYPE_FREETEXT = 0;
    private static AUTHOR_TYPE_PERSON = 1;
    private lastMdsQuery: string;
    dialogTitle: string;
    dialogMessage: string;
    dialogParameters: any;
    dialogButtons: DialogButton[];
    private variables: string[];
    currentWidgetSuggestion: string;
    private static GROUP_MULTIVALUE_DELIMITER = '[+]';
    private mdsId = new Date().getTime();
    private childobjectDrag: number;
    private initialValues: any;
    @Input() set suggestions(suggestions: any) {
        this._suggestions = suggestions;
        this.applySuggestions();
    }
    @Input() set repository(repository: string) {
        this.isLoading = false;
        this._repository = repository;
    }
    @Input() set currentValues(currentValues: any) {
        this._currentValues = currentValues;
    }
    @Input() set setId(setId: string) {
        if (!setId) return;
        this._setId = setId;
    }
    @Input() set invalidate(invalidate: Boolean) {
        if (invalidate && invalidate.valueOf()) setTimeout(() => this.loadMds(), 5);
    }

    @Input() set groupId(groupId: string) {
        this._groupId = groupId;
    }
    private isSearch() {
        return this._groupId == RestConstants.DEFAULT_QUERY_NAME;
    }

    private loadMdsFinal(callback: Function = null) {
        if (!this.mds) return;
        this.renderGroup(this._groupId, this.mds);
        this.isLoading = false;
        this.setValuesByProperty(this.mds, this._currentValues ? this._currentValues : {});
        setTimeout(() => {
            this.showExtended(this.extended);
            this.onMdsLoaded.emit(this.mds);
            if (callback) callback();
        }, 5);
    }
    loadMds() {
        if (this.isLoading) {
            setTimeout(() => this.loadMds(), 5);
            return;
        }
        this.mds = null;
        this.rendered = null;
        this.renderedSuggestions = null;
        this.isLoading = true;
        this.mdsService.getSet(this._setId, this._repository).subscribe(
            (data: any) => {
                this.locator.getConfigVariables().subscribe(
                    (variables: string[]) => {
                        this.mds = data;
                        this.variables = variables;
                        this.loadMdsFinal();
                    },
                    (error: any) => this.toast.error(error),
                );
            },
            (error: any) => this.toast.error(error),
        );
    }

    /**
     * Set this to a special type if you want to first create a special node (e.g. a tool definition)
     * @param {string} type
     */
    @Input() set create(type: string) {
        this.createType = type;
        //@TODO: This causes a race condition! But it's not used at the moment anyway
        /*
    this.isLoading=true;
    this.mdsService.getSet().subscribe((data:any)=> {
      this.mds=data;
      this.renderGroup(this.createType, this.mds);
      this.isLoading=false;
    });
    */
    }

    @Input() set nodes(nodes: Node[]) {
        this.currentNodes = null;
        if (nodes == null) {
            return;
        }
        this.isLoading = true;
        this.mdsEditorCommon.fetchNodesMetadata(nodes).then(async (nodesConverted) => {
            let mdsId: string;
            let mdsDefinition: MdsDefinition;
            try {
                mdsId = this.mdsEditorCommon.getMdsId(nodesConverted)
                mdsDefinition = await this.mdsEditorCommon.fetchMdsDefinition(mdsId);
            } catch (error) {
                if (error instanceof UserPresentableError) {
                    this.toast.error(null, error.message);
                } else {
                    this.toast.error(error);
                }
                this.cancel();
            }
            this._setId = mdsId;
            this.mds = mdsDefinition;
            this.onMdsLoaded.emit(mdsDefinition);
            this.currentNodes = nodesConverted;
            if (
                nodesConverted[0].type === RestConstants.CCM_TYPE_IO &&
                nodesConverted.length === 1
            ) {
                this.node.getNodeChildobjects(nodesConverted[0].ref.id).subscribe(
                    (childs: NodeList) => {
                        this.currentChildobjects = childs.nodes;
                        this.loadConfig();
                    },
                    (error: any) => {
                        this.toast.error(error);
                        this.cancel();
                    },
                );
            } else {
                this.loadConfig();
            }
        });
    }
    @Output() onCancel = new EventEmitter();
    @Output() onDone = new EventEmitter();
    @Output() openLicense = new EventEmitter();
    @Output() openTemplate = new EventEmitter();
    @Output() openContributor = new EventEmitter();
    @Output() onMdsLoaded = new EventEmitter();
    rendered: SafeHtml;
    renderedSuggestions: SafeHtml;
    jumpmarks: CardJumpmark[];
    isLoading = false;

    private widgetName = 'cclom:general_keyword';
    private widgetType = 'multivalueFixedBadges';
    currentNodes: Node[];
    public addChildobject = false;
    public editChildobject: any;
    public editChildobjectLicense: any;
    private currentChildobjects: Node[] = [];
    private childobjects: any = [];
    public globalProgress = false;
    private properties: string[] = [];
    currentWidgets: any[];
    private mds: any;
    private static MAX_SUGGESTIONS = 5;
    private suggestionsViaSearch = false;
    buttons = [
        new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.cancel()),
        new DialogButton('SAVE', DialogButton.TYPE_PRIMARY, () => this.saveValues()),
    ];

    resetValues() {
        this._currentValues = null;
        this.loadMdsFinal(() => {
            this.onDone.emit(null);
        });
    }

    constructor(
        private mdsService: RestMdsService,
        private translate: TranslateService,
        private route: ActivatedRoute,
        private uiService: UIService,
        private node: RestNodeService,
        private tools: RestToolService,
        private utilities: RestUtilitiesService,
        private toast: Toast,
        private locator: RestLocatorService,
        private storage: SessionStorageService,
        private connector: RestConnectorService,
        private sanitizer: DomSanitizer,
        private config: ConfigurationService,
        private mdsEditorCommon: MdsEditorCommonService,
        private nodeHelper: NodeHelperService,
        private _ngZone: NgZone,
    ) {
        //Translation.initialize(this.translate,this.config,this.storage,this.route);
        (window as any)['mdsComponentRef_' + this.mdsId] = { component: this, zone: _ngZone };
    }

    ngOnDestroy() {
        (window as any)['mdsComponentRef_' + this.mdsId];
    }
    getWindowComponent() {
        return 'window.mdsComponentRef_' + this.mdsId + '.component';
    }
    private openSuggestions(
        id: string,
        event: any,
        allowCustom: boolean,
        widgetValues: boolean,
        showMore = false,
        search = this.suggestionsViaSearch,
    ) {
        let widget = this.getWidget(id);
        this.suggestionsViaSearch = search;
        let searchField: any = document.getElementById(
            this.getWidgetDomId(widget) + '_suggestionsInput',
        );
        if (allowCustom) {
            if (event && event.keyCode == 13 && searchField.value != '') {
                let badges = document.getElementById(this.getWidgetDomId(widget));
                let elements: any = badges.childNodes;
                let add = true;
                for (let i = 0; i < elements.length; i++) {
                    if (elements[i].getAttribute('data-value') == searchField.value) {
                        add = false;
                    }
                }
                if (add) {
                    badges.innerHTML += this.getMultivalueBadge(searchField.value);
                }
                searchField.value = null;
            }
        }
        let list = document.getElementById(this.getWidgetDomId(widget) + '_suggestions');
        list.className = list.className.replace('suggestionListAll', '').trim();
        if (showMore) {
            list.className += ' suggestionListAll';
        }
        let elements = list.getElementsByTagName('a');
        if (event && event.keyCode == 40) {
            elements[elements.length > 1 ? 1 : 0].focus();
            return;
        }
        let more = elements[elements.length - 1];
        more.style.display = 'none';

        list.style.display = 'none';

        if (searchField.value.length < 2 && search) return;
        this.currentWidgetSuggestion = this.getWidgetDomId(widget);
        list.style.display = '';
        let hits = 0;
        let moreCount = 0;
        for (let i = 1; i < elements.length - 1; i++) {
            let element = elements[i];
            element.style.display = 'none';
            let caption = element.getAttribute('data-caption');
            let pos = -1;

            if (caption && search)
                pos = caption.toLowerCase().indexOf(searchField.value.toLowerCase());
            else {
                pos = 0;
            }
            if (pos == -1) {
                continue;
            }
            if (hits >= MdsComponent.MAX_SUGGESTIONS && !showMore) {
                moreCount++;
                continue;
            }
            element.style.display = pos > -1 ? '' : 'none';
            element.innerHTML = caption;
            if (search) {
                element.innerHTML = this.highlightSearch(caption, searchField.value);
            }
            hits += pos > -1 ? 1 : 0;
        }
        if (moreCount) {
            more.style.display = '';
            more.innerHTML = moreCount + ' ' + this.translate.instant('MORE_SELECTBOX');
        }
        // Commented part fetches from repo, however this won't work for all mds properly
        if (!widgetValues) this.mdsUpdateSuggests(id);
        if (widgetValues /* && !this._groupId */)
            elements[0].style.display = hits || allowCustom ? 'none' : '';
    }
    /*
  private renderTabs(group:any,data:any,node:Node) : string{
    let tabs='<div class="row tab-group"><ul class="tabs">';
    let content='';
    let i=0;

    for(let viewId of group.views){
      for(let view of data.views){
        if(view.id==viewId){
          tabs+=`<li class="tab clickable" id="mds_tab_header_`+i+`" onclick="
                  for(var i=0;true;i++){
                    var element=document.getElementById('mds_tab_'+i);
                    var header=document.getElementById('mds_tab_header_'+i);
                    if(!element)
                      return;
                    var selected=i==`+i+`;
                    element.style.display=selected ? '' : 'none';
                    header.childNodes[0].className=selected ? 'active' : 'none';
                 }

                    "><a class="`+(i==0 ? 'active' : '')+'">'+view.caption+'</a></li>';
          content+='<div id="mds_tab_'+i+'"'+(i>0 ? 'style="display:none;"' : '')+'>'+this.renderTemplate(view,data,null,node)+'</div>';
          i++;
          break;
        }
      }
    }
    tabs+='</ul></div>';
    return tabs+content;
  }
  */
    private renderList(group: any, data: any): any {
        let content = '';
        let i = 0;
        let hasExtended = [false];
        let result: any = { main: '' };
        for (let viewId of group.views) {
            let viewFound = false;
            for (let view of data.views) {
                if (view.id == viewId) {
                    viewFound = true;
                    if (!this.embedded && view.caption)
                        result.main +=
                            `<div class="card-title-element" id="` +
                            view.id +
                            `_header"><i class="material-icons">` +
                            view.icon +
                            `</i>` +
                            view.caption +
                            `</div>`;
                    if (view.rel) {
                        if (!result[view.rel]) result[view.rel] = '';
                        result[view.rel] += this.renderTemplate(view, data, hasExtended);
                    } else {
                        result.main += this.renderTemplate(view, data, hasExtended);
                    }
                    i++;
                    break;
                }
            }
            if (!viewFound) {
                result.main += 'View ' + viewId + ' was not found in the list of known views.';
            }
        }
        if (hasExtended[0]) {
            let extended =
                `<div class="mdsExtended ` +
                (this.isSearch() ? 'mdsExtendedSearch' : '') +
                `"><div class="label">` +
                this.translate.instant(
                    this.isSearch() ? 'MDS.SHOW_EXTENDED_SEARCH' : 'MDS.SHOW_EXTENDED',
                ) +
                `</div><div class="switch">
            <label>
              ` +
                this.translate.instant('OFF') +
                `
                <input type="checkbox" id="` +
                this.getDomId('mdsExtendedCheckbox') +
                `" onchange="` +
                this.getWindowComponent() +
                `.showExtended(this.checked)">
                <span class="lever"></span>
              ` +
                this.translate.instant('ON') +
                `
              </label>
          </div></div>`;
            result.main = extended + result.main;
        }
        return result;
    }
    private showExtended(show: boolean) {
        this.extended = show;
        this.extendedChange.emit(show);
        let checkbox = document.getElementById(
            this.getDomId('mdsExtendedCheckbox'),
        ) as HTMLInputElement;
        if (!checkbox) return;
        checkbox.checked = show;
        let display = 'none';
        if (show) {
            display = '';
        }
        let elements: any = document.getElementsByClassName('mdsExtendedGroup');
        for (let i = 0; i < elements.length; i++) {
            elements[i].style.display = display;
        }
    }

    private renderJumpmarks(group: any, data: any) {
        this.jumpmarks = [];
        for (let viewId of group.views) {
            for (let view of data.views) {
                if (view.id == viewId) {
                    this.jumpmarks.push(
                        new CardJumpmark(view.id + '_header', view.caption, view.icon),
                    );
                    break;
                }
            }
        }
    }

    private renderGroup(id: string, data: any) {
        if (!id) return;
        this.currentWidgets = [];
        // add the default widgets
        data.widgets.push({ id: 'preview' });
        data.widgets.push({ id: 'version' });
        data.widgets.push({
            id: 'childobjects',
            caption: this.translate.instant('MDS.ADD_CHILD_OBJECT'),
        });
        data.widgets.push({ id: 'template' });
        data.widgets.push({ id: 'workflow', caption: this.translate.instant('MDS.WORKFLOW') });
        data.widgets.push({ id: 'author', caption: this.translate.instant('MDS.AUTHOR_LABEL') });
        data.widgets.push({
            id: RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR,
            type: 'vcard',
        });
        data.widgets.push({ id: RestConstants.CCM_PROP_AUTHOR_FREETEXT, type: 'textarea' });
        if (this.getWidget('license', null, data.widgets) == null) {
            data.widgets.push({ id: 'license', caption: this.translate.instant('MDS.LICENSE') });
        }
        for (let group of data.groups) {
            if (group.id == id) {
                let result = this.renderList(group, data);
                this.setRenderedHtml(result.main);
                if (result.suggestions)
                    this.renderedSuggestions = this.sanitizer.bypassSecurityTrustHtml(
                        result.suggestions,
                    );
                this.renderJumpmarks(group, data);
                this.readValues(data);
                //setTimeout(()=>UIHelper.materializeSelect(),15);
                return;
            }
        }
        let html = "Group '" + id + "' was not found in the mds";
        this.setRenderedHtml(html);
    }

    /**
     * checks if the user has made modifications to the original/initial state
     */
    public isDirty() {
        return !Helper.objectEquals(this.initialValues, this.getValues());
    }
    public getValues(propertiesIn: any = {}, showError = true, widgets = this.currentWidgets) {
        let properties: any = {};
        // add author data
        this.addAuthorValue(properties);
        if (!widgets) return properties;
        for (let widget of widgets) {
            if (widget.id == 'preview' || widget.id == 'author') {
                continue;
            }
            let domId = this.getWidgetDomId(widget);
            if (widget.type == 'vcard') {
                if (!propertiesIn[widget.id]) propertiesIn[widget.id] = [null];

                let vcard = new VCard(propertiesIn[widget.id][0]);
                for (let field of MdsComponent.VCARD_FIELDS) {
                    let element = document.getElementById(domId + '_' + field) as any;
                    if (!element) continue;
                    vcard.set(field, element.value);
                }
                propertiesIn[widget.id][0] = vcard.toVCardString();
                properties[widget.id] = propertiesIn[widget.id];
                continue;
            }
            if (widget.type == 'radioVertical' || widget.type == 'radioHorizontal') {
                properties[widget.id] = [];
                let list: any = document.getElementsByName(domId);
                for (let i = 0; i < list.length; i++) {
                    if (list.item(i).checked) {
                        properties[widget.id].push(list.item(i).value);
                    }
                }
                continue;
            }
            let element = document.getElementById(domId) as any;
            if (!element) {
                continue;
            }
            if (widget.type == 'checkboxVertical' || widget.type == 'checkboxHorizontal') {
                let inputs = element.getElementsByTagName('input');
                properties[widget.id] = [];
                for (let input of inputs) {
                    if (input.checked) {
                        properties[widget.id].push(input.value);
                    }
                }
                continue;
            }
            if (
                (this.isPrimitiveWidget(widget) ||
                    widget.type === 'textarea' ||
                    widget.type === 'singleoption') &&
                element.disabled
            ) {
                // disabled => ignore property
                continue;
            }
            element.className = element.className.replace('invalid', '').trim();
            let v = element.value;
            if (element.getAttribute('data-value') && !this.isPrimitiveWidget(widget)) {
                v = element.getAttribute('data-value');
            }
            let props = [v];
            if (this.isSliderWidget(widget)) {
                if (element.getAttribute('disabled') === 'true') {
                    continue;
                }
                let value = element.noUiSlider.get();
                let valueClean = [];
                for (let v of Array.isArray(value) ? value : [value]) {
                    let split = (v + '').split('</label>');
                    v = split[split.length - 1].trim();
                    if (widget.unit) v = v.replace(widget.unit, '').trim();
                    valueClean.push(v);
                }

                if (widget.type == 'duration') {
                    valueClean[0] *= 60000;
                }
                if (widget.type == 'range') {
                    properties[widget.id + '_from'] = [valueClean[0]];
                    properties[widget.id + '_to'] = [valueClean[1]];
                    continue;
                }
                if (Array.isArray(valueClean)) props = valueClean;
                else props = [valueClean];
            } else if (this.isMultivalueWidget(widget)) {
                props = [];
                if (this.isMultivalueWidget(widget) && this.isBulkMode()) {
                    const mode = element.getAttribute('data-bulk');
                    if (mode === 'replace') {
                        // keep an empty property array -> replace
                    } else {
                        // add props
                        if (propertiesIn[widget.id]) {
                            props = propertiesIn[widget.id];
                        }
                    }
                }
                for (let i = 0; i < element.childNodes.length; i++) {
                    const e = element.childNodes.item(i) as HTMLElement;
                    const value = e.getAttribute('data-value');
                    if (props.indexOf(value) === -1) {
                        props.push(value);
                    }
                }
            } else if (widget.type == 'checkbox') {
                props = [(element as any).checked];
            }
            if (this.isRequiredWidget(widget) && (!props.length || props[0] == '')) {
                if (showError) {
                    let inputField = element;
                    if (this.isMultivalueWidget(widget)) {
                        inputField = document.getElementById(domId + '_suggestionsInput');
                    }
                    if (inputField) inputField.className += 'invalid';
                    this.toast.error(null, 'TOAST.FIELD_REQUIRED', { name: widget.caption });
                }
                return;
            }
            if (this.isSearch()) {
                // don't send empty values to search -> this may not work with defaultvalues, so keep it
                if (!props || (props.length == 1 && !props[0] && !widget.defaultvalue)) continue;
                if (props.length == 1 && props[0] == '') props = [];
            }
            properties[widget.id] = props;
        }
        if (!properties[RestConstants.CM_NAME]) {
            if (this.isBulkMode()) {
                properties[RestConstants.CM_NAME] = propertiesIn[RestConstants.CM_NAME];
            } else {
                properties[RestConstants.CM_NAME] = properties[RestConstants.LOM_PROP_TITLE];
            }
        }
        return properties;
    }
    private checkFileExtension(name: string, callback: () => void = null, values: any) {
        if (values == null) {
            return true;
        }
        let ext1 = name.split('.');
        let ext2 = values[RestConstants.CM_NAME][0].split('.');
        let extV1 = ext1[ext1.length - 1];
        let extV2 = ext2[ext2.length - 1];
        if (ext1.length == 1 && ext2.length == 1) return true;
        if (extV1 != extV2) {
            this.dialogTitle = 'EXTENSION_NOT_MATCH';
            this.dialogMessage = 'EXTENSION_NOT_MATCH_INFO';
            if (ext1.length == 1) {
                this.dialogMessage = 'EXTENSION_NOT_MATCH_INFO_NEW';
            }
            if (ext2.length == 1) {
                this.dialogMessage = 'EXTENSION_NOT_MATCH_INFO_OLD';
            }
            this.dialogParameters = {
                extensionOld: extV1,
                extensionNew: extV2,
                warning: this.translate.instant('EXTENSION_NOT_MATCH_WARNING'),
            };
            this.dialogButtons = [
                new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => {
                    this.dialogTitle = null;
                }),
                new DialogButton('SAVE', DialogButton.TYPE_PRIMARY, () => {
                    this.dialogTitle = null;
                    this.saveValues(callback, true);
                }),
            ];
            return false;
        }
        return true;
    }
    public saveValues(callback: () => void = null, force = false) {
        let properties: any = {};
        if (this.currentNodes) {
            properties = this.currentNodes[0].properties;
        }
        const values = this.getValues(properties);
        // check if file extension changed and warn
        if (!force) {
            // for regular nodes
            if (
                this.currentNodes &&
                this.currentNodes[0] &&
                this.currentNodes.length === 1 &&
                this.currentNodes[0].type === RestConstants.CCM_TYPE_IO &&
                !this.currentNodes[0].properties[RestConstants.CCM_PROP_IO_WWWURL] &&
                !this.checkFileExtension(this.currentNodes[0].name, callback, values)
            ) {
                return;
            }
            // for childobjects
            if (
                this._groupId === MdsType.IoChildObject &&
                !this._currentValues[RestConstants.CCM_PROP_IO_WWWURL] &&
                !this.checkFileExtension(
                    this._currentValues[RestConstants.CM_NAME][0],
                    callback,
                    values,
                )
            ) {
                return;
            }
        }
        if (this.embedded || (this.currentNodes == null && this.createType == null)) {
            this.onDone.emit(this.getValues());
            return this.getValues();
        }

        let version = '';
        let files: File[] = [];
        try {
            const comment = document.getElementById('comment') as any;
            version = comment.value;
            files = (document.getElementById('fileSelect') as any).files;
            const display = document.getElementById('versionGroup').style.display;
            if (!version.trim()) {
                throw new Error();
            }
        } catch (e) {
            if (files.length) {
                version = RestConstants.COMMENT_CONTENT_UPDATE;
            } else {
                version = RestConstants.COMMENT_METADATA_UPDATE;
            }
        }

        this.globalProgress = true;
        // can only happen for single element
        if (files.length) {
            this.node.editNodeMetadata(this.currentNodes[0].ref.id, properties).subscribe(
                (node) => {
                    this.currentNodes[0] = node.node;
                    this.node
                        .uploadNodeContent(this.currentNodes[0].ref.id, files[0], version)
                        .subscribe(
                            () => {
                                this.onUpdatePreview(callback);
                            },
                            (error: any) => {
                                this.toast.error(error);
                                this.globalProgress = false;
                            },
                        );
                },
                (error: any) => {
                    this.toast.error(error);
                    this.globalProgress = false;
                },
            );
        } else {
            // can be bulk mode
            Observable.forkJoin(
                this.currentNodes.map((n) =>
                    this.node.editNodeMetadataNewVersion(
                        n.ref.id,
                        version,
                        this.getValues(n.properties),
                    ),
                ),
            ).subscribe(
                (nodes) => {
                    this.currentNodes = nodes.map((n) => n.node);
                    this.onUpdatePreview(callback);
                },
                (error) => {
                    this.toast.error(error);
                    this.globalProgress = false;
                },
            );
        }
    }
    public setValuesByProperty(data: any, properties: any) {
        setTimeout(() => {
            for (let widget of data.widgets) {
                if (widget.template) continue;
                let props = properties[widget.id];
                let element = document.getElementById(this.getWidgetDomId(widget)) as any;
                // try to resolve proper template widget if exists to exchange valuespace
                try {
                    let template = element.parentNode.getAttribute('data-template');
                    if (template != null) {
                        let tplWidget = this.getWidget(widget.id, template);
                        if (tplWidget) widget = tplWidget;
                    }
                } catch (e) {}
                if (widget.id == 'author') {
                    if (
                        properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] &&
                        properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR][0] &&
                        new VCard(
                            properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR][0],
                        ).getDisplayName()
                    ) {
                        this.setActiveAuthor(MdsComponent.AUTHOR_TYPE_PERSON);
                    } else this.setActiveAuthor(MdsComponent.AUTHOR_TYPE_FREETEXT);

                    //this.setActiveAuthor(MdsComponent.AUTHOR_TYPE_FREETEXT);
                }
                if (widget.type == 'vcard') {
                    if (!props) continue;

                    let vcard = new VCard(props[0]);
                    for (let field of MdsComponent.VCARD_FIELDS) {
                        let element = document.getElementById(
                            this.getWidgetDomId(widget) + '_' + field,
                        ) as any;
                        if (element) {
                            element.value = vcard.get(field);
                        }
                    }
                } else if (element) {
                    if (this.isSliderWidget(widget)) {
                        if (
                            widget.type == 'range' &&
                            properties[widget.id + '_from'] &&
                            properties[widget.id + '_to']
                        ) {
                            let from = properties[widget.id + '_from'][0];
                            let to = properties[widget.id + '_to'][0];
                            element.noUiSlider.set([from, to]);
                        }
                        if (!props) continue;
                        if (widget.type == 'duration') {
                            props[0] /= 60000;
                        }
                        element.noUiSlider.set(props);
                    }
                    if (!props) continue;

                    if (widget.type == 'multivalueGroup') {
                        for (let v of props) {
                            if (v != '') {
                                let caption = this.getGroupValueCaption(v, widget);
                                element.innerHTML += this.getMultivalueBadge(v, caption);
                            }
                        }
                    } else if (this.isMultivalueWidget(widget)) {
                        for (let v of props) {
                            if (v != '') {
                                let caption = this.getValueCaption(widget, v);
                                element.innerHTML += this.getMultivalueBadge(v, caption);
                            }
                        }
                    } else if (widget.type == 'checkbox') {
                        element.checked = props[0] == 'true';
                    } else if (
                        widget.type == 'checkboxVertical' ||
                        widget.type == 'checkboxHorizontal'
                    ) {
                        for (let input of element.getElementsByTagName('input')) {
                            input.checked = props.indexOf(input.value) != -1;
                        }
                    } else if (widget.type == 'singleoption') {
                        element.value = props[0] ? props[0] : '';
                    } else if(widget.type == 'date') {
                        element.value=props[0] ? DateHelper.formatDateByPattern(props[0],'y-M-d') : '';
                    } else {
                        let caption = props[0];
                        if (widget.values) {
                            for (let value of widget.values) {
                                if (value.id == caption) {
                                    caption = value.caption ? value.caption : caption;
                                    break;
                                }
                            }
                        }
                        element.value = caption;
                        try {
                            let event = new KeyboardEvent('keyup', {
                                view: window,
                                bubbles: true,
                                cancelable: true,
                            });
                            // simulate event for materialize
                            element.dispatchEvent(event);
                        } catch (e) {
                            // fails in ie11
                        }
                        if (element.value !== props[0]) {
                            element.setAttribute('data-value', props[0]);
                        }
                    }
                } else {
                    if (!props) continue;
                    for (let v of props) {
                        let element = document.getElementById(
                            this.getWidgetDomId(widget) + '_' + v,
                        ) as any;
                        if (element) {
                            if (element.type == 'checkbox' || element.type == 'radio')
                                element.checked = true;
                        }
                    }
                }
            }
        }, 10);
    }

    private getValueCaption(widget: any, id: string) {
        if (widget.values) {
            for (let value of widget.values) {
                if (value.id === id) {
                    return value.caption ? value.caption : id;
                }
            }
        }
        return id;
    }
    private readValues(data: any) {
        this.setGeneralNodeData();
        this.setValuesByProperty(data, this.currentNodes ? this.getMergedProperties() : []);
    }
    private renderTemplate(template: any, data: any, extended: boolean[]) {
        if (!template.html || !template.html.trim()) {
            return '';
        }
        let html =
            '<div class="mdsGroup' + (this.embedded ? ' mdsEmbedded' : '') + '">' + template.html;
        let removeWidgets: any = [];
        for (let widget of data.widgets) {
            if (!widget.template) {
                for (let w2 of data.widgets) {
                    if (w2.id == widget.id && w2.template == template.id) {
                        widget = w2;
                        break;
                    }
                }
            } else {
                continue; // already processed!
            }
            let search = '<' + widget.id + '>';
            let start = html.indexOf(search);
            let end = start + search.length;
            let attr = '';
            if (start < 0) {
                search = '<' + widget.id + ' ';
                start = html.indexOf(search);
                end = html.indexOf('>', start) + 1;
                attr = html.substring(start + search.length, end - 1);
                let attributes = this.getAttributes(html.substring(start, end - 1));
                for (var k in attributes) {
                    widget[k] = attributes[k];
                }
            }
            if (start < 0) continue;

            let first = html.substring(0, start);
            let second = html.substring(end);

            if (this.isExtendedWidget(widget)) extended[0] = true;
            this.replaceVariables(widget);
            this.currentWidgets.push(widget);
            let widgetData = this.renderWidget(widget, attr, template);
            if (!widgetData) {
                removeWidgets.push(widget);
                continue;
            }
            html = first + widgetData + second;
        }
        for (let remove of removeWidgets) {
            html = html.replace(new RegExp('<' + remove.id + '*>'), '');
        }
        html += '</div>';
        return html;
    }
    private isBulkMode() {
        return this.currentNodes && this.currentNodes.length > 1;
    }
    private addBulkCheckbox(widget: any) {
        if (!this.isBulkMode()) {
            return '';
        }
        const id = this.getDomId(widget.id + '_bulk');
        return (
            `<div class="bulk-enable switch" ` +
            (this.bulkBehaviour === BulkBehavior.Replace ? 'style="display:none"' : '') +
            `>
                <label>
                  ` +
            this.translate.instant('MDS.BULK_OVERRIDE') +
            `
                  <input type="checkbox" id="` +
            id +
            `" 
                    onchange="` +
            this.getWindowComponent() +
            `.toggleBulk('` +
            widget.id +
            `', event)"
                    ` +
            (this.bulkBehaviour === BulkBehavior.Replace ? 'checked' : '') +
            `>
                  <span class="lever"></span>
                 </label>
               </div>`
        );
    }
    private addBulkMode(widget: any) {
        if (!this.isBulkMode()) {
            return '';
        }
        const id = this.getDomId(widget.id + '_bulk');
        return (
            `<div class="bulk-enable" ` +
            (this.bulkBehaviour === BulkBehavior.Replace ? 'style="display:none"' : '') +
            `>
                <input type="radio" name="` +
            id +
            `" id="` +
            id +
            `_append" value="append" onchange="` +
            this.getWindowComponent() +
            `.setBulkMode('` +
            widget.id +
            `', event)"` +
            (this.bulkBehaviour === BulkBehavior.Default ? 'checked' : '') +
            `>
                <label for="` +
            id +
            `_append">` +
            this.translate.instant('MDS.BULK_APPEND') +
            `</label>
                <input type="radio" name="` +
            id +
            `" id="` +
            id +
            `_replace" value="replace" onchange="` +
            this.getWindowComponent() +
            `.setBulkMode('` +
            widget.id +
            `', event)"` +
            (this.bulkBehaviour === BulkBehavior.Replace ? 'checked' : '') +
            `>
                <label for="` +
            id +
            `_replace">` +
            this.translate.instant('MDS.BULK_REPLACE') +
            `</label>
            </div>`
        );
    }
    toggleBulk(widget: string, event: any) {
        const widgetData = this.getWidget(widget);
        const element = document.getElementById(this.getDomId(widget)) as HTMLInputElement;
        if (widgetData.type === 'slider' || widgetData.type === 'range') {
            if (event.srcElement.checked) {
                element.removeAttribute('disabled');
            } else {
                element.setAttribute('disabled', 'true');
            }
        } else {
            element.disabled = !event.srcElement.checked;
        }
    }
    setBulkMode(widget: string, event: any) {
        (document.getElementById(this.getDomId(widget)) as HTMLInputElement).setAttribute(
            'data-bulk',
            event.srcElement.value,
        );
    }
    private renderPrimitiveWidget(widget: any, attr: string, type: string, css = '') {
        let html = '<div class="inputTable">';
        html += this.addBulkCheckbox(widget);
        if (widget.icon) {
            html += '<i class="inputIcon material-icons">' + widget.icon + '</i>';
        }
        html +=
            '<input type="' +
            type +
            '" ' +
            'id="' +
            this.getWidgetDomId(widget) +
            '" ' +
            'placeholder="' +
            (widget.placeholder ? widget.placeholder : '') +
            '" ' +
            'value="' +
            (widget.defaultvalue ? widget.defaultvalue : '') +
            '" ' +
            'class="' +
            css +
            '"';
        if (this.isBulkMode() && this.bulkBehaviour !== BulkBehavior.Replace) {
            html += ' disabled';
        }
        html += '>';
        if (widget.type == 'checkbox') {
            html += this.getCaption(widget);
        }
        html += '</div>';
        html += this.addBottomCaption(widget);
        return html;
    }
    private getSuggestBadge(value: string, caption: string, id: string) {
        return (
            `<div tabindex="0" class="badge" data-value="` +
            value +
            `" onclick="
            this.parentNode.removeChild(this);
            var caption='` +
            caption +
            `';
            var value='` +
            value +
            `';
            document.getElementById('` +
            id +
            `').innerHTML+='` +
            this.getMultivalueBadgeEmbedded('caption', 'value') +
            `';
            "
            onkeyup="
             if (event.keyCode === 13) {
               this.parentNode.removeChild(this);
              var caption='` +
            caption +
            `';
              var value='` +
            value +
            `';
              document.getElementById('` +
            id +
            `').innerHTML+='` +
            this.getMultivalueBadgeEmbedded('caption', 'value') +
            `';
             }
            "
            ><i class="material-icons clickable">add_circle</i>
            <span>` +
            caption +
            `</span>
            </div>`
        );
    }
    private getMultivalueBadge(value: string, caption: string = value) {
        return (
            '<div class="badge" data-value="' +
            value +
            '"><span>' +
            caption +
            `</span><i class="material-icons clickable" tabindex="0" onkeyup="if(event.keyCode==13){this.click()}" onclick="
    this.parentNode.parentNode.removeChild(this.parentNode);
    ` +
            this.getWindowComponent() +
            `.applySuggestions();
    ">cancel</i></div>`
        );
    }
    private getMultivalueBadgeEmbedded(label = 'this.value', value = 'this.value') {
        return (
            `<div class=\\'badge\\' data-value=\\''+` +
            value +
            `+'\\'><span>'+` +
            label +
            `+'</span><i class=\\'material-icons clickable\\' tabindex=\\'0\\' onkeyup=\\'if(event.keyCode==13){this.click()}\\' onclick=\\'this.parentNode.parentNode.removeChild(this.parentNode);` +
            this.getWindowComponent() +
            `.applySuggestions();\\'>cancel</i></div>`
        );
    }
    private renderVCardWidget(widget: any, attr: string) {
        let html = '<div class="vcard">';
        let i = 0;
        for (let field of MdsComponent.VCARD_FIELDS) {
            let id = this.getWidgetDomId(widget) + '_' + field;
            let caption = this.translate.instant('VCARD.' + field);
            html += `<div class="vcardGroup">`;
            if (i == 0) {
                html += `<i class="material-icons">person</i>`;
            }
            html +=
                `<div class="vcard_group_`+field+`"><label for="` +
                id +
                `">` +
                caption +
                `</label>
               <input type="text" class="vcard_` +
                field +
                `" id="` +
                id +
                `">`;

            html += `</div></div>`;
            i++;
        }
        html += '</div>';
        return html;
    }
    private renderMultivalueBadgesWidget(widget: any, attr: string) {
        let html =
            `<input type="text" class="multivalueBadgesText" aria-label="` +
            widget.caption +
            `" placeholder="` +
            (widget.placeholder ? widget.placeholder : '') +
            `" onkeyup="if(event.keyCode==13){
        var elements=document.getElementById('` +
            widget.id +
            `').childNodes;
        for(var i=0;i<elements.length;i++){
            if(elements[i].getAttribute('data-value',undefined,undefined,undefined,htmlElement)==this.value){
                return;
            }
        }
        document.getElementById('` +
            widget.id +
            `').innerHTML+='` +
            this.getMultivalueBadgeEmbedded() +
            `';
        this.value='';
      }
      ">`;
        html += this.addBottomCaption(widget);
        html += `<div id="` + widget.id + `" class="multivalueBadges"></div>`;
        return html;
    }
    private renderSuggestBadgesWidget(widget: any, attr: string, allowCustom: boolean) {
        let html =
            this.autoSuggestField(
                widget,
                '',
                allowCustom,
                this.getWindowComponent() +
                    `.openSuggestions('` +
                    widget.id +
                    `',null,false,` +
                    (widget.values ? true : false) +
                    `,false,false)`,
            ) +
            `<div id="` +
            this.getWidgetDomId(widget) +
            `" class="multivalueBadges"></div>`;
        return html;
    }
    toggleTreeItem(from: HTMLElement, widgetId: string, id: string) {
        const element = document.getElementById(this.getDomId(widgetId) + '_group_' + id);
        const enable = element.style.display === 'none';
        element.style.display = enable ? '' : 'none';
        from.innerHTML = enable ? 'keyboard_arrow_down' : 'keyboard_arrow_right';
    }
    private renderSubTree(widget: any, parent: string = null) {
        const values = widget.valuesTree?.[parent];
        if (!values) {
            return null;
        }
        let html =
            '<div id="' + this.getWidgetDomId(widget) + '_group_' + parent + '" class="treeGroup"';
        if (parent != null) {
            html += ' style="display:none;"';
        }
        html += '>';
        for (let value of values) {
            let id = this.getWidgetDomId(widget) + '_' + value.id;
            let sub = null;
            if (widget.valuesTree?.[value.id]) {
                sub = this.renderSubTree(widget, value.id);
            }
            html += '<div><div id="' + id + '_bg"><div class="treeIcon">';
            if (sub) {
                html +=
                    `<i class="material-icons clickable" onclick="` +
                    this.getWindowComponent() +
                    `.toggleTreeItem(this,'` +
                    widget.id +
                    `','` +
                    value.id +
                    `')">keyboard_arrow_right</i>`;
            } else {
                html += '&nbsp;';
            }
            html +=
                `</div><input type="checkbox" id="` +
                id +
                `" class="filled-in" onchange="` +
                this.getWindowComponent() +
                `.changeTreeItem(this,'` +
                widget.id +
                `')"`;
            if (value.disabled) {
                html += ' disabled="true"';
            }
            html +=
                ' value="' +
                value.id +
                '"><label for="' +
                id +
                '">' +
                value.caption +
                '</label></div>';
            if (sub) {
                html += sub;
            }
            html += '</div>';
        }
        html += '</div>';
        return html;
    }
    private changeTreeItem(element: any, widgetId: string) {
        let widget = this.getWidget(widgetId);
        let multivalue = widget.type == 'multivalueTree';
        document.getElementById(element.id + '_bg').className = element.checked
            ? 'treeSelected'
            : '';
        if (!multivalue) {
            let inputs = document
                .getElementById(this.getWidgetDomId(widget) + '_tree')
                .getElementsByTagName('input');
            for (let i = 0; i < inputs.length; i++) {
                if (inputs[i].id == element.id) continue;
                inputs[i].checked = false;
                document.getElementById(inputs[i].id + '_bg').className = '';
            }
        }
        if (this.mode == 'search') {
            // disable all sub-elements if the element is checked, because they will all be found as well
            let subElements = element.parentElement.parentElement.getElementsByTagName('input');
            for (let i = 0; i < subElements.length; i++) {
                if (subElements.item(i) == element) continue;
                subElements.item(i).disabled = element.checked;
                subElements.item(i).checked = element.checked;
                document.getElementById(subElements.item(i).id + '_bg').className = element.checked
                    ? 'treeSelected'
                    : '';
            }
        }
    }
    public handleKeyboardEvent(event: KeyboardEvent) {
        if (event.code == 'Escape') {
            if (this.addChildobject) {
                this.addChildobject = false;
                event.preventDefault();
                event.stopPropagation();
                return true;
            }
            if (this.editChildobject) {
                this.editChildobject = null;
                event.preventDefault();
                event.stopPropagation();
                return true;
            }
            for (let widget of this.currentWidgets) {
                if (widget.type == 'multivalueTree') {
                    let element = document.getElementById(widget.id + '_tree');
                    if (element && element.style.display == '') {
                        element.style.display = 'none';
                        event.preventDefault();
                        event.stopPropagation();
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private mdsUpdateSuggests(id: string, showMore = false) {
        let widget = this.getWidget(id);
        let list = document.getElementById(this.getWidgetDomId(widget) + '_suggestions');
        let element: any = document.getElementById(
            this.getWidgetDomId(widget) + '_suggestionsInput',
        );
        let elements = list.getElementsByTagName('a');
        if (showMore) {
            list.className += ' suggestionListAll';
        }
        elements.item(0).style.display = 'none';
        list.style.display = 'none';
        let values = this.getValues([], false);
        let group = this._groupId;
        if (!group || group === 'io_simple') {
            group = RestConstants.DEFAULT_QUERY_NAME;
        }
        this.lastMdsQuery = element.value;
        this.mdsService
            .getValues(
                {
                    valueParameters: {
                        query: group,
                        property: id,
                        pattern: element.value,
                    },
                    criterias:this.mode==='search' ?
                        RestSearchService.convertCritierias(
                            Helper.arrayJoin(this._currentValues,this.getValues()),this.mds.widgets
                        ) : null,
                },this._setId,this._repository).subscribe((data:MdsValueList)=>{
                  if(this.lastMdsQuery!=element.value)
                    return;

                    for (let i = 1; i < elements.length; ) {
                        list.removeChild(elements.item(i));
                    }
                    list.className = list.className.replace('suggestionListAll', '').trim();

                    list.style.display = '';
                    let i = 0;
                    let moreCount = 0;
                    for (let value of data.values) {
                        if (i >= MdsComponent.MAX_SUGGESTIONS && !showMore) {
                            moreCount++;
                            continue;
                        }
                        let key = value.key ? value.key : value.displayString;
                        let caption = this.getKeyCaption(key, widget.values);
                        if (!widget.values && value.displayString) {
                            caption = value.displayString;
                        }
                        list.innerHTML += this.getListEntry(
                            this.getWidgetDomId(widget),
                            key,
                            caption,
                            false,
                            element.value,
                        );
                        i++;
                    }
                    if (i == 0) {
                        list.style.display = 'none';
                    }
                    if (moreCount) {
                        list.innerHTML +=
                            '<a class="collection-item suggestionMoreItems" onclick="' +
                            this.getWindowComponent() +
                            ".mdsUpdateSuggests('" +
                            id +
                            '\',true)">' +
                            moreCount +
                            ' ' +
                            this.translate.instant('MORE_SELECTBOX') +
                            '</a>';
                    }
                    //elements.item(0).style.display=data.values ? 'none' : '';
                },
                (error: any) => {
                    console.warn('invalid suggestions result for ' + this._groupId + ' ' + id);
                },
            );
    }
    handleListKeyDown(event: any, element: HTMLElement, id: string) {
        if (event.keyCode === 13) {
            element.click();
        }
        const suggestions = document.getElementById(id + '_suggestions');
        const elements = suggestions.childNodes as any;
        if (event.keyCode === 40 || event.keyCode === 38) {
            const direction = event.keyCode === 40 ? 1 : -1;
            for (let i = 0; i < elements.length; i++) {
                if (elements[i] === element) {
                    let pos = i + direction;
                    while (
                        pos > 0 &&
                        pos < elements.length &&
                        elements[pos].style.display === 'none'
                    )
                        pos += direction;
                    if (pos === 0) pos = elements.length - 1;
                    if (pos === elements.length) pos = 1;
                    elements[pos].focus();
                    event.preventDefault();
                    event.stopPropagation();
                    return;
                }
            }
        }
    }
    handleListClick(element: HTMLElement, id: string, singleValue: boolean) {
        document.getElementById(id + '_suggestions').style.display = 'none';
        this.currentWidgetSuggestion = null;
        (document.getElementById(id + '_suggestionsInput') as HTMLInputElement).value = '';

        if (!this.uiService.isMobile()) {
            document.getElementById(id + '_suggestionsInput').focus();
        }
        const badges = document.getElementById(id);
        const elements = badges.childNodes as any;
        for (let i = 0; i < elements.length; i++) {
            if (elements[i].getAttribute('data-value') === element.getAttribute('data-value')) {
                return;
            }
        }
        const data = this.getMultivalueBadge(
            element.getAttribute('data-value'),
            element.getAttribute('data-caption'),
        );
        if (singleValue) {
            badges.innerHTML = data;
        } else {
            badges.innerHTML += data;
        }
    }
    private getListEntry(
        id: string,
        key: string,
        caption: string,
        singleValue = false,
        searchString: string = null,
    ) {
        let html =
            `<a class="collection-item" tabindex="0" data-value="` +
            key +
            `" data-caption="` +
            this.htmlEscape(caption) +
            `"
                onkeydown="` +
            this.getWindowComponent() +
            `.handleListKeyDown(event, this, '` +
            id +
            `')"
                onclick="` +
            this.getWindowComponent() +
            `.handleListClick(this, '` +
            id +
            `', ` +
            singleValue +
            `)"`;

        html +=
            `>` + (searchString ? this.highlightSearch(caption, searchString) : caption) + `</a>`;
        return html;
    }
    private autoSuggestField(
        widget: any,
        css = '',
        allowCustom = false,
        openCallback: string,
        openIcon = 'arrow_drop_down',
        singleValue = false,
    ) {
        let html = '';
        html += this.addBulkMode(widget);
        if (widget.values == null /* || this._groupId*/) {
            openCallback = null;
        }
        if (!openCallback && widget.type != 'multivalueTree' && widget.type != 'singlevalueTree') {
            css += ' suggestInputNoOpen';
        }
        let postfix = '_suggestionsInput';

        html +=
            `<div class="auto-suggest-field"><input type="text" id="` +
            this.getWidgetDomId(widget) +
            postfix +
            `" `;
        html +=
            `aria-label="` +
            widget.caption +
            `" placeholder="` +
            (widget.placeholder ? widget.placeholder : '') +
            `" class="suggestInput ` +
            css +
            `" 
            onkeyup="` +
            this.getWindowComponent() +
            `.openSuggestions('` +
            widget.id +
            `',event,` +
            allowCustom +
            `,` +
            (widget.values ? true : false) +
            `,false,true)">`;
        if (widget.type == 'singleoption' && !widget.allowempty) {
            setTimeout(() => {
                let pos = 0;
                if (widget.defaultvalue) {
                    for (let i = 0; i < widget.values.length; i++) {
                        if (widget.values[i].id == widget.defaultvalue) {
                            pos = i;
                            break;
                        }
                    }
                }
                eval(
                    `
          document.getElementById('` +
                        widget.id +
                        `').value='` +
                        widget.values[pos].caption +
                        `';
          document.getElementById('` +
                        widget.id +
                        `').setAttribute('data-value','` +
                        widget.values[pos].id +
                        `');
        `,
                );
            }, 5);
        }
        if (openCallback) {
            html +=
                `<a class="btn-flat suggestOpen" 
              onclick="` +
                openCallback +
                `"
              `;
            html += `"><i class="material-icons">` + openIcon + `</i></a>`;
        }
        html += `</div>`;
        html += this.addBottomCaption(widget);
        html +=
            `<div id="` +
            this.getWidgetDomId(widget) +
            `_suggestions" class="suggestionList collection" style="display:none;">`;

        html +=
            `<a class="collection-item suggestionNoMatches"  onclick="
              document.getElementById('` +
            this.getWidgetDomId(widget) +
            `_suggestions').style.display='none';
              document.getElementById('` +
            this.getWidgetDomId(widget) +
            `_dialog').style.display='none';
              ">` +
            this.translate.instant('NO_MATCHES') +
            `</a>`;
        if (widget.allowempty == true) {
            html += this.getListEntry(this.getWidgetDomId(widget), '', '', singleValue);
        }
        if (widget.values) {
            for (const value of widget.values) {
                if (value.disabled) {
                    continue;
                }
                html += this.getListEntry(
                    this.getWidgetDomId(widget),
                    value.id,
                    value.caption,
                    singleValue,
                );
            }
        }
        html +=
            `<a class="collection-item suggestionMoreItems"  onclick="
              //document.getElementById('` +
            widget.id +
            `_suggestions').style.display='none';
              //document.getElementById('` +
            widget.id +
            `_dialog').style.display='none';
              ` +
            this.getWindowComponent() +
            `.openSuggestions('` +
            widget.id +
            `',null,false,` +
            (widget.values ? true : false) +
            `,true);
              ">...</a>`;
        html += `</div>`;
        if (!widget.bottomCaption) {
            if (allowCustom && !openCallback) {
                html +=
                    '<div class="input-hint-bottom">' +
                    this.translate.instant('WORKSPACE.EDITOR.HINT_ENTER') +
                    '</div>';
            } else {
                html +=
                    '<div class="input-hint-bottom">' +
                    this.translate.instant('WORKSPACE.EDITOR.HINT_FILTER') +
                    '</div>';
            }
        }
        return html;
    }
    closeDialog() {
        document.getElementById(this.currentWidgetSuggestion + '_suggestions').style.display =
            'none';
        this.currentWidgetSuggestion = null;
    }
    openTree(widget: string) {
        let id = this.getDomId(widget);
        let tree = document.getElementById(id + '_tree');
        tree.style.display = '';
        let childs = document.getElementById(id).childNodes;
        let elements = tree.getElementsByTagName('input');
        let selectedBg = tree.getElementsByClassName('treeSelected');
        // reset old data
        for (let i = 0; i < elements.length; i++) {
            elements[i].checked = false;
        }
        while (selectedBg.length > 0) {
            selectedBg[0].className = '';
        }

        // mark current values
        for (let i = 0; i < childs.length; i++) {
            let child: any = childs[i];
            let element: any = document.getElementById(id + '_' + child.getAttribute('data-value'));
            if (element) {
                element.checked = true;
                this.changeTreeItem(element, widget);
            }
        }
    }
    private renderTreeWidget(widget: any, attr: string) {
        const constrain = this.handleWidgetConstrains(widget, {
            requiresNode: false,
            supportsBulk: widget.type === 'multivalueTree',
        });
        if (constrain) {
            return constrain;
        }
        let domId = this.getWidgetDomId(widget);
        // transform tree for faster access (about 4x speedup for large trees)
        if (widget.values) {
            widget.valuesTree = {};
            for (const value of widget.values) {
                if (!widget.valuesTree[value.parent]) {
                    widget.valuesTree[value.parent] = [];
                }
                widget.valuesTree[value.parent].push(value);
            }
        }
    let html=this.autoSuggestField(widget,'',false,
                this.getWindowComponent()+`.openTree('`+widget.id+`')`,'arrow_forward',widget.type=='singlevalueTree')
        +`     <div class="dialog darken mds-tree-dialog" style="display:none;z-index:`+(122 + this.priority)+`;" id="`+domId+`_tree">
                <div class="card center-card card-wide card-high card-action">
                  <div class="card-content">
                  <div class="card-cancel" onclick="document.getElementById('` +
            domId +
            `_tree').style.display='none';"><i class="material-icons">close</i></div>
                  <div class="card-title">` +
            (widget.caption ? widget.caption : widget.placeholder) +
            `</div>
                    <div class="card-scroll">
                    ` +
            this.renderSubTree(widget, null) +
            `
                    </div>
                  </div>
                  <div class="card-action">
                       <a class="waves-effect waves-light btn" onclick="` +
            this.getWindowComponent() +
            `.saveTree('` +
            widget.id +
            `')">` +
            this.translate.instant('SAVE') +
            `</a>
                     </div>
                </div>
              </div>
              <div id="` +
            domId +
            `" class="multivalueBadges"></div>`;
        // delete existing tree from document
        try {
            document
                .getElementsByTagName('body')[0]
                .removeChild(document.getElementById(domId + '_tree'));
        } catch (e) {}
        // dirty hack: In search, the tree is inside the sidebar which does not render correctly. So we need to append it to the main body and delete any existing trees
        setTimeout(() => {
            try {
                let id = domId + '_tree';
                document.getElementsByTagName('body')[0].appendChild(document.getElementById(id));
            } catch (e) {}
        }, 5);
        return html;
    }
    private saveTree(widgetId: string) {
        let widget = this.getWidget(widgetId);
        let tree = document.getElementById(this.getWidgetDomId(widget) + '_tree');
        tree.style.display = 'none';
        let badges = document.getElementById(this.getWidgetDomId(widget));
        while (badges.firstChild) badges.removeChild(badges.firstChild);
        let elements = tree.getElementsByTagName('input');
        let labels = tree.getElementsByTagName('label');
        for (let i = 0; i < elements.length; i++) {
            let element = elements[i];
            let label = labels[i];
            if (!element.checked || element.disabled) continue;
            badges.innerHTML += this.getMultivalueBadge(element.value, label.innerHTML);
        }
    }
    private renderTextareaWidget(widget: any, attr: string) {
        let html = '';
        html += this.addBulkCheckbox(widget);
        html += '<textarea class="materialize-textarea" id="' + this.getWidgetDomId(widget) + '"';
        if (this.isBulkMode() && this.bulkBehaviour !== BulkBehavior.Replace) {
            html += ' disabled';
        }
        if (widget.placeholder) {
            html += ' placeholder="' + widget.placeholder + '"';
        }
        if (widget.maxlength) {
            html += ' maxlength="' + widget.maxlength + '"';
        }
        html += '></textarea>';
        return html;
    }
    private renderDurationWidget(widget: any, attr: string) {
        const constrain = this.handleWidgetConstrains(widget, {
            requiresNode: false,
            supportsBulk: false,
        });
        if (constrain) {
            return constrain;
        }
        let id = this.getWidgetDomId(widget);
        let html =
            `
              <div class="inputField"><label for="` +
            id +
            `_hours">` +
            this.translate.instant('INPUT_HOURS') +
            `</label>
              <input type="number" min="0" max="9" id="` +
            id +
            `_hours" onchange="
              document.getElementById('` +
            id +
            `').noUiSlider.set(
              document.getElementById('` +
            id +
            `_hours').value*60+
              document.getElementById('` +
            id +
            `_minutes').value*1);
              " />
              </div>
              <div class="inputField"><span>:</span></div>
              <div class="inputField">
              <label for="` +
            id +
            `_minutes">` +
            this.translate.instant('INPUT_MINUTES') +
            `</label>
              <input type="number" min="0" max="60" id="` +
            id +
            `_minutes" onchange="
              document.getElementById('` +
            id +
            `').noUiSlider.set(
              document.getElementById('` +
            id +
            `_hours').value*60+
              document.getElementById('` +
            id +
            `_minutes').value*1);
              "/>
              </div>
              <div class="inputSlider" id="` +
            id +
            `"></div>
    `;
        setTimeout(() => {
            eval(
                `
                var slider = document.getElementById('` +
                    id +
                    `');
                          noUiSlider.create(slider, {
                           start: [0],
                           step: 1,
                           connect: true,
                           tooltips: true,
                           format: {
                            to: function ( value ) {
                              //return Math.round(value/60)+':'+Math.round(value%60);
                              return '<label>` +
                    this.translate.instant('INPUT_MINUTES') +
                    `</label>'+Math.round(value);
                            },from:function(value){return value;}
                            },
                           range: {
                             'min': 0,
                             'max': 599
                           },
                          });
                          var sliderUpdate=function(values,handle,unencoded){
                    document.getElementById('` +
                    id +
                    `_hours').value=Math.floor(unencoded/60);
                    document.getElementById('` +
                    id +
                    `_minutes').value=Math.floor(unencoded%60);
                  };
                  slider.noUiSlider.on('slide', sliderUpdate);
                  slider.noUiSlider.on('update', sliderUpdate);
            `,
            );
        }, 5);
        return html;
    }
    private renderRangeWidget(widget: any, attr: string) {
        let html = '';
        html += this.addBulkCheckbox(widget);
        let id = this.getWidgetDomId(widget);
        html +=
            `
      <div class="inputRange" id="` +
            id +
            `"></div>
    `;
        setTimeout(() => {
            let values = widget.defaultvalue != null ? widget.defaultvalue : widget.min;
            if (widget.type === 'range') {
                values = [
                    widget.defaultMin != null ? widget.defaultMin : widget.min,
                    widget.defaultMax != null ? widget.defaultMax : widget.max,
                ];
            }
            const unit = widget.unit ? widget.unit : '';
            const slider = document.getElementById(id);
            noUiSlider.create(slider, {
                start: values,
                step: widget.step > 1 ? widget.step : 1,
                connect: true,
                tooltips: true,
                disabled: true,
                format: {
                    to: (value: any) => {
                        return Math.round(value) + ' ' + unit;
                    },
                    from: (value: any) => {
                        return value;
                    },
                },
                range: {
                    min: widget.min,
                    max: widget.max,
                },
            });
            if (this.isBulkMode() && this.bulkBehaviour !== BulkBehavior.Replace) {
                slider.setAttribute('disabled', 'true');
            }
        }, 5);
        return html;
    }

    private renderSingleoptionWidget(widget: any, attr: string) {
        let html = '';
        html += this.addBulkCheckbox(widget);
        if (widget.values == null) {
            return (
                'Error at ' + widget.id + ': No values for a singleoption widget is not possible'
            );
        }
        html += '<select id="' + this.getWidgetDomId(widget) + '"';
        if (this.isBulkMode() && this.bulkBehaviour !== BulkBehavior.Replace) {
            html += ' disabled';
        }
        html += '>';
        if (widget.allowempty == true) {
            html += '<option value=""></option>';
        }
        for (let option of widget.values) {
            html += '<option value="' + option.id + '"';
            if (widget.defaultvalue && option.id == widget.defaultvalue) {
                html += ' selected';
            }
            html += '>' + option.caption + '</option>';
        }
        html += '</select>';
        html += this.addBottomCaption(widget);
        return html;
    }
    private renderMultioptionWidget(widget: any, attr: string) {
        let html =
            `<select onchange="
        var elements=document.getElementById('` +
            this.getWidgetDomId(widget) +
            `').childNodes;
        for(var i=0;i<elements.length;i++){
            if(elements[i].getAttribute('data-value',undefined,undefined,undefined,htmlElement)==this.value){
                return;
            }
        }
        document.getElementById('` +
            this.getWidgetDomId(widget) +
            `').innerHTML+='` +
            this.getMultivalueBadgeEmbedded('this.options[this.selectedIndex].innerHTML') +
            `';
        this.value='';
      "><option></option>`;
        for (let option of widget.values) {
            html += '<option value="' + option.id + '">' + option.caption + '</option>';
        }
        html +=
            '</select><div id="' +
            this.getWidgetDomId(widget) +
            '" class="multivalueBadges"></div>';
        return html;
    }
    private renderRadioWidget(widget: any, attr: string, vertical: boolean) {
        let html = '<fieldset class="' + (vertical ? 'radioVertical' : 'radioHorizontal') + '">';

        for (let option of widget.values) {
            let id = this.getWidgetDomId(widget) + '_' + option.id;
            html +=
                '<input type="radio" name="' +
                this.getWidgetDomId(widget) +
                '" id="' +
                id +
                '" value="' +
                option.id +
                '"' +
                (option.id == widget.defaultvalue ? ' checked' : '') +
                (option.disabled ? ' disabled' : '') +
                '> <label for="' +
                id +
                '">' +
                option.caption +
                '</label>';
        }
        html += '</fieldset>';
        return html;
    }
    private renderCheckboxWidget(widget: any, attr: string, vertical: boolean) {
        let html =
            '<fieldset id="' +
            this.getWidgetDomId(widget) +
            '" class="' +
            (vertical ? 'checkboxVertical' : 'checkboxHorizontal') +
            '">';

        for (let option of widget.values) {
            let id = this.getWidgetDomId(widget) + '_' + option.id;
            html +=
                '<input type="checkbox" class="filled-in" name="' +
                id +
                '" id="' +
                id +
                '" value="' +
                option.id +
                '"' +
                (option.disabled ? ' disabled' : '') +
                '> <label for="' +
                id +
                '">' +
                (option.imageSrc ? '<img src="' + option.imageSrc + '">' : '') +
                (option.caption ? '<span class="caption">' + option.caption + '</span>' : '') +
                (option.description
                    ? '<span class="description">' + option.description + '</span>'
                    : '') +
                '</label>';
        }
        html += '</fieldset>';
        return html;
    }
    private isWidgetConditionTrue(widget: any) {
        return MdsHelper.isWidgetConditionTrue(this.connector, widget, this.getCurrentProperties());
    }
    private renderWidget(widget: any, attr: string, template: any): string {
        let id = widget.id;
        if (widget.id === 'version') {
            widget.caption = this.translate.instant('WORKSPACE.EDITOR.VERSION');
        }
        let hasCaption = widget.caption;
        let html = '';
        let caption = '';
        if (!this.isWidgetConditionTrue(widget)) return null;

        if (hasCaption) {
            caption = this.getCaption(widget);
        }
        let idLong = widget.id + (template.rel ? '_' + template.rel : '') + '_container';
        html += '<div id="' + idLong + '" class="' + idLong + '';
        if (this.isExtendedWidget(widget)) {
            html += ' mdsExtendedGroup" style="display:none"';
        } else {
            html += '"';
        }
        html += '>';
        if (widget.type != 'checkbox') html += caption;

        html +=
            '<div class="mdsWidget widget_' +
            widget.type +
            ' ' +
            id.replace(':', '_') +
            '"' +
            attr +
            ' data-template="' +
            template.id +
            '">';
        if (template.rel == 'suggestions') {
            html +=
                `<div id="` +
                this.getWidgetDomId(widget) +
                `_badgeSuggestions" style="display:none" class="multivalueBadges"></div>`;
        } else if (this.isPrimitiveWidget(widget)) {
            html += this.renderPrimitiveWidget(widget, attr, widget.type);
        } else if (widget.type == 'textarea') {
            html += this.renderTextareaWidget(widget, attr);
        } else if (widget.type == 'duration') {
            html += this.renderDurationWidget(widget, attr);
        } else if (widget.type == 'range' || widget.type == 'slider') {
            html += this.renderRangeWidget(widget, attr);
        } else if (widget.type == 'singleoption') {
            html += this.renderSingleoptionWidget(widget, attr);
        } else if (widget.type == 'multioption') {
            html += this.renderMultioptionWidget(widget, attr);
        } else if (widget.type == 'radioHorizontal' || widget.type == 'radioVertical') {
            html += this.renderRadioWidget(widget, attr, widget.type == 'radioVertical');
        } else if (widget.type == 'checkboxHorizontal' || widget.type == 'checkboxVertical') {
            html += this.renderCheckboxWidget(widget, attr, widget.type == 'checkboxVertical');
        } else if (widget.type == 'multivalueBadges') {
            html += this.renderSuggestBadgesWidget(widget, attr, true);
        } else if (
            widget.type == 'multivalueSuggestBadges' ||
            widget.type == 'multivalueFixedBadges'
        ) {
            html += this.renderSuggestBadgesWidget(
                widget,
                attr,
                widget.type == 'multivalueSuggestBadges',
            );
        } else if (widget.type == 'multivalueTree' || widget.type == 'singlevalueTree') {
            html += this.renderTreeWidget(widget, attr);
        } else if (widget.type == 'checkbox') {
            html += this.renderPrimitiveWidget(widget, attr, widget.type, 'filled-in');
        } else if (widget.type == 'vcard') {
            html += this.renderVCardWidget(widget, attr);
        } else if (widget.type == 'multivalueGroup') {
            html += this.renderGroupWidget(widget, attr, template);
        } else if (widget.type == 'defaultvalue') {
            // hide this widget, it's used in backend
            return '';
        } else if (widget.id == 'preview') {
            html += this.renderPreview(widget, attr);
        } else if (widget.id == 'author') {
            html += this.renderAuthor(widget);
        } else if (widget.id == 'version') {
            html += this.renderVersion(widget);
        } else if (widget.id == 'childobjects') {
            html += this.renderChildobjects(widget);
        } else if (widget.id == 'license') {
            html += this.renderLicense(widget);
        } else if (widget.id == 'workflow') {
            html += this.renderWorkflow(widget);
        } else if (widget.id == 'template') {
            html += this.renderTemplateWidget(widget);
        } else {
            html += "Unknown widget type '" + widget.type + "' at id '" + widget.id + "'";
        }

        html += '</div></div>';
        return html;
    }

    private getCaption(widget: any) {
        let caption = '<label for="' + this.getWidgetDomId(widget) + '"> ' + widget.caption;
        if (this.isRequiredWidget(widget))
            caption +=
                ' <span class="required">(' + this.translate.instant('FIELD_REQUIRED') + ')</span>';
        caption += '</label>';
        return caption;
    }

    private getAttributes(element: string) {
        let attributes: any = {};
        let str = element;
        while (true) {
            str = str.substring(str.indexOf(' ') + 1);
            let pos = str.indexOf('=');
            if (pos == -1) {
                return attributes;
            }
            let name = str.substring(0, pos).trim();
            str = str.substring(pos + 1);
            let search = ' ';
            if (str.startsWith("'")) {
                search = "'";
            }
            if (str.startsWith('"')) {
                search = '"';
            }
            if (search != ' ') str = str.substring(1);
            let end = str.indexOf(search);
            let value = str.substring(0, end);
            str = str.substring(end + 1);
            attributes[name] = value;
        }
    }

    private isMultivalueWidget(widget: any) {
        return (
            widget.type === 'multivalueBadges' ||
            widget.type === 'multioption' ||
            widget.type === 'multivalueFixedBadges' ||
            widget.type === 'multivalueSuggestBadges' ||
            widget.type === 'singlevalueTree' || // it basically uses the tree so all functions relay on multivalue stuff
            widget.type === 'multivalueTree' ||
            widget.type === 'multivalueGroup'
        );
    }
    private isSliderWidget(widget: any) {
        return widget.type === 'duration' || widget.type === 'range' || widget.type === 'slider';
    }
    private addBottomCaption(widget: any) {
        if (widget.bottomCaption) {
            return '<div class="input-hint-bottom">' + widget.bottomCaption + '</div>';
        }
        return '';
    }
    private renderAuthor(widget: any) {
        const constrain = this.handleWidgetConstrains(widget, {
            requiresNode: false,
            supportsBulk: false,
        });
        if (constrain) {
            return constrain;
        }
        let authorWidget = { id: RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR };
        let freetextWidget = {
            id: RestConstants.CCM_PROP_AUTHOR_FREETEXT,
            placeholder: this.translate.instant('MDS.AUTHOR_FREETEXT_PLACEHOLDER'),
        };
        let author =
            `
      <div class="mdsAuthor">
        <div class="row">
          <div class="col s12">
          <ul class="tabs" id="` +
            this.getDomId('mdsAuthorTabs') +
            `">
            <li class="clickable tab col s6" onclick="` +
            this.getWindowComponent() +
            `.setActiveAuthor(` +
            MdsComponent.AUTHOR_TYPE_FREETEXT +
            `)">
              <a>` +
            this.translate.instant('MDS.AUTHOR_FREETEXT') +
            `</a>
            </li>
            <li class="clickable tab col s6" onclick="` +
            this.getWindowComponent() +
            `.setActiveAuthor(` +
            MdsComponent.AUTHOR_TYPE_PERSON +
            `)">
              <a>` +
            this.translate.instant('MDS.AUTHOR_PERSON') +
            `</a>
            </li>
          </ul>
         </div>
         <div id="` +
            this.getDomId('mdsAuthorFreetext') +
            `" class="mdsAuthorFreetext">` +
            this.renderTextareaWidget(freetextWidget, null) +
            `</div>
          <div id="` +
            this.getDomId('mdsAuthorPerson') +
            `" class="mdsAuthorPerson">` +
            this.renderVCardWidget(authorWidget, null);
        if (this.currentNodes && this.currentNodes.length === 1 && !this.uiService.isMobile()) {
            author +=
                `<div class="mdsContributors">
            <a class="clickable contributorsLink" onclick="` +
                this.getWindowComponent() +
                `.openContributorsDialog();">` +
                this.translate.instant('MDS.CONTRIBUTOR_LINK') +
                ` <i class="material-icons">arrow_forward</i></a>
          </div>`;
        }
        author += `
         </div>
        </div>
      </div>
    `;
        return author;
    }
    private changePreview(element: any) {
        let valid = element.files.length;
        if (valid) {
            document
                .getElementById(this.getDomId('preview'))
                .setAttribute('data-custom', true as any);
            (document.getElementById(
                this.getDomId('preview'),
            ) as any).src = window.URL.createObjectURL(element.files[0]);
            document.getElementById(this.getDomId('preview-deleted')).style.display = 'none';
            document.getElementById(this.getDomId('preview-delete')).style.display = null;
        }
    }
    private deletePreview() {
        (document.getElementById(this.getDomId('preview-select')) as any).files = null;
        document.getElementById(this.getDomId('preview-deleted')).style.display = null;
        document.getElementById(this.getDomId('preview-delete')).style.display = 'none';
    }
    private handleWidgetConstrains(
        widget: any,
        options: { requiresNode?: boolean; supportsBulk?: boolean },
    ) {
        const name = widget.type || widget.id;
        if (options.requiresNode && !this.currentNodes?.length) {
            return "Widget '" + name + "' is only supported if a node object is available";
        } else if (!options.supportsBulk && this.isBulkMode()) {
            return "Widget '" + name + "' is not supported in bulk mode";
        }
        return null;
    }
    private renderPreview(widget: any, attr: string) {
        const constrain = this.handleWidgetConstrains(widget, {
            requiresNode: true,
            supportsBulk: false,
        });
        if (constrain) {
            return constrain;
        }
        let preview = `<div class="mdsPreview">`;

        preview +=
            `<input type="file" style="display:none" id="` +
            this.getDomId('preview-select') +
            `" accept="image/*" onchange="` +
            this.getWindowComponent() +
            `.changePreview(this)" />
              <label>` +
            this.translate.instant('WORKSPACE.EDITOR.PREVIEW') +
            `</label>`;
        preview +=
            `<div class="previewImage">
              <div id="` +
            this.getDomId('preview-deleted') +
            `" class="preview-deleted" style="display:none">
                  <i class="material-icons">delete</i><div>` +
            this.translate.instant('WORKSPACE.EDITOR.PREVIEW_DELETED') +
            `</div>
              </div>`;
        preview += `<img id="` + this.getDomId('preview') + `" ` + attr + ` alt=""></div>`;
        if (this.connector.getApiVersion() >= RestConstants.API_VERSION_4_0) {
            preview +=
                `<div class="changePreview">
                      <a tabindex="0"
                      onclick="document.getElementById('` +
                this.getDomId('preview-select') +
                `').click()" 
                      onkeydown="if(event.keyCode==13)this.click();" class="btn-circle"><i class="material-icons" aria-label="` +
                this.translate.instant('WORKSPACE.EDITOR.REPLACE_PREVIEW') +
                `">file_upload</i></a>
                          <a tabindex="0"
                          id="` +
                this.getDomId('preview-delete') +
                `"
                          ` +
                (this.currentNodes[0].preview.isGenerated ? 'style="display:none"' : '') +
                `
                          onclick="` +
                this.getWindowComponent() +
                `.deletePreview()" 
                          onkeydown="if(event.keyCode==13) this.click();"
                          class="btn-circle"><i class="material-icons" aria-label="` +
                this.translate.instant('WORKSPACE.EDITOR.DELETE_PREVIEW') +
                `">delete</i></a>
                      </div>`;
        }
        preview += `</div>`;
        return preview;
    }
    private setEditChildobject(pos: number) {
        this.editChildobject = {
            child: this.childobjects[pos],
            properties: this.getChildobjectProperties(this.childobjects[pos], pos),
        };
    }
    private setEditChildobjectLicense(pos: number) {
        this.editChildobjectLicense = {
            child: this.childobjects[pos],
            properties: this.getChildobjectProperties(this.childobjects[pos], pos),
        };
    }
    private removeChildobject(pos: number) {
        let element = document
            .getElementById(this.getDomId('mdsChildobjects'))
            .getElementsByClassName('childobject')
            .item(pos);
        document.getElementById(this.getDomId('mdsChildobjects')).removeChild(element);
        this.childobjects.splice(pos, 1);
        this.refreshChildobjects();
    }
    private startChildobjectDrag(event: any, pos: number) {
        this.childobjectDrag = pos;
        event.dataTransfer.effectAllowed = 'all';
    }
    private childobjectDragOver(event: any, posNew: number) {
        Helper.arraySwap(this.childobjects, this.childobjectDrag, posNew);
        this.childobjectDrag = posNew;
        this.refreshChildobjects();
    }
    private renderChildObject(data: any, pos: number) {
        let list = document.getElementById(this.getDomId('mdsChildobjects'));
        list.innerHTML +=
            `
        <div class="childobject"
        draggable="true"
        ondragstart="` +
            this.getWindowComponent() +
            `.startChildobjectDrag(event,` +
            pos +
            `)"
        ondragover="` +
            this.getWindowComponent() +
            `.childobjectDragOver(event,` +
            pos +
            `)">
            <div class="icon"><img src="` +
            data.icon +
            `"></div>
            <div class="name">` +
            data.name +
            `</div>
            <div class="license"><i onclick="` +
            this.getWindowComponent() +
            `.setEditChildobjectLicense(` +
            pos +
            `)" class="material-icons clickable">copyright</i></div>
            <div class="edit"><i onclick="` +
            this.getWindowComponent() +
            `.setEditChildobject(` +
            pos +
            `)" class="material-icons clickable">edit</i></div>
            <div class="remove"><i onclick="` +
            this.getWindowComponent() +
            `.removeChildobject(` +
            pos +
            `)" class="material-icons clickable">remove_circle_outline</i></div>
        </div>
    `;
    }
    refreshChildobjects() {
        let list = document.getElementById(this.getDomId('mdsChildobjects'));
        if (!list) return;
        list.innerHTML = '';
        let i = 0;
        for (let child of this.childobjects) {
            this.renderChildObject(child, i);
            i++;
        }
    }
    setCurrentChildobjects() {
        for (let child of this.currentChildobjects) {
            this.childobjects.push({
                icon: child.iconURL,
                name: RestHelper.getTitle(child),
                node: child,
                properties: child.properties,
            });
        }
        this.refreshChildobjects();
    }
    addChildobjectLink(event: any) {
        let link = this.nodeHelper.addHttpIfRequired(event.link);
        this.addChildobject = false;
        let properties = RestHelper.createNameProperty(link);
        properties[RestConstants.CCM_PROP_IO_WWWURL] = [link];
        properties[RestConstants.LOM_PROP_TITLE] = [link];
        let process = () => {
            let data: any = {
                icon: this.connector.getThemeMimeIconSvg('link.svg'),
                name: RestHelper.getTitleFromProperties(properties),
                link: link,
                properties: properties,
            };
            this.childobjects.push(data);
            this.refreshChildobjects();
        };
        this.utilities.getWebsiteInformation(link).subscribe(
            (info) => {
                if (info.title)
                    properties[RestConstants.LOM_PROP_TITLE] = [info.title + ' - ' + info.page];
                process();
            },
            (error) => {
                console.warn(error);
                process();
            },
        );
    }
    addChildobjectFile(event: any) {
        this.addChildobject = false;
        for (let file of event) {
            let child = {
                icon: RestHelper.guessMediatypeIconForFile(this.connector, file),
                name: file.name,
                file: file,
            };
            this.childobjects.push(child);
        }
        this.refreshChildobjects();
    }
    private renderChildobjects(widget: any) {
        const constrain = this.handleWidgetConstrains(widget, {
            requiresNode: false,
            supportsBulk: false,
        });
        if (constrain) {
            return constrain;
        }
        let html =
            `<div class="mdsChildobjects">
      <div class="label-light">` +
            this.translate.instant('MDS.ADD_CHILD_OBJECT_DESCRIPTION') +
            `</div>
      <input type="file" style="display:none" id="childSelect" onchange="` +
            this.getWindowComponent() +
            `.addChildobject(this)" />
      <div class="list" id="` +
            this.getDomId('mdsChildobjects') +
            `"></div>
      <a class="btn-flat btn-shadow waves-light waves-effect btn-icon" onclick="` +
            this.getWindowComponent() +
            `.addChildobject=true">
          <i class="material-icons">add</i> ` +
            this.translate.instant('ADD') +
            `
      </a>
      </div>
      `;
        return html;
    }
    private renderVersion(widget: any) {
        if (!this.allowReplacing) return '';
        const constrain = this.handleWidgetConstrains(widget, {
            requiresNode: true,
            supportsBulk: true,
        });
        if (constrain) {
            return constrain;
        }
        let html = `<div class="mdsVersion">
          <input type="file" style="display:none" id="fileSelect" onchange="
            var valid=this.files.length;
            if(valid){
              document.getElementById('selectedFileContent').innerHTML=this.files[0].name;
            }
            document.getElementById('selectedFile').style.display=valid ? '' : 'none';
            document.getElementById('selectFileBtn').style.display=valid ? 'none' : '';
          " />
            <div class="version">`;
        if (this.isContentEditable() && !this.isBulkMode()) {
            html +=
                `<div class="btn-flat btn-shadow" id="selectFileBtn" onclick="document.getElementById('fileSelect').click()">` +
                this.translate.instant('WORKSPACE.EDITOR.REPLACE_MATERIAL') +
                `</div>`;
        }
        html +=
            `
              <div id="selectedFile" class="badge" style="display:none;"><span id="selectedFileContent"></span>
              <i class="material-icons clickable" onclick="
              document.getElementById('fileSelect').value = null;
              document.getElementById('selectedFile').style.display='none';
              document.getElementById('selectFileBtn').style.display='';
              ">cancel</i></div>

            <div id="versionGroup">
            <input type="text" class="comment" id="comment" placeholder="` +
            this.translate.instant('WORKSPACE.EDITOR.VERSION_COMMENT') +
            `" required />
              <div class="input-hint-bottom"` +
            this.translate.instant('FIELD_MUST_BE_FILLED') +
            `</div>
            </div>
           </div>
          </div>
         </div>`;
        return html;
    }
    private openLicenseDialog() {
        this.saveValues(() => {
            this.openLicense.emit();
        });
    }
    private openTemplateDialog() {
        this.saveValues(() => {
            this.openTemplate.emit();
        });
    }
    private openContributorsDialog() {
        this.saveValues(() => {
            this.openContributor.emit();
        });
    }
    private getGroupValueCaption(value: string, widget: any) {
        let values = value.split(MdsComponent.GROUP_MULTIVALUE_DELIMITER);
        let caption = '';
        let i = 0;
        for (let sub of widget.subwidgets) {
            let v = values[i++];
            if (!v) continue;
            if (caption != '') {
                caption += ', ';
            }
            caption += this.getValueCaption(this.getWidget(sub.id), v);
        }
        return caption;
    }
    private addGroupValues(id: string) {
        let widget = this.getWidget(id);
        let widgets = [];
        for (let sub of widget.subwidgets) {
            widgets.push(this.getWidget(sub.id));
        }
        let values = this.getValues([], true, widgets);
        if (!values) return;
        let result = '';
        let i = 0;
        let hasValue = false;
        for (let sub of widget.subwidgets) {
            if (values[sub.id] && values[sub.id][0]) {
                hasValue = true;
                result += values[sub.id][0];
            }
            if (i++ < widget.subwidgets.length - 1)
                result += MdsComponent.GROUP_MULTIVALUE_DELIMITER;
        }
        if (!hasValue) {
            return;
        }
        let badges = document.getElementById(this.getWidgetDomId(widget));
        let elements: any = badges.childNodes;
        let add = true;
        for (let i = 0; i < elements.length; i++) {
            if (elements[i].getAttribute('data-value') == result) {
                return;
            }
        }
        let caption = this.getGroupValueCaption(result, widget);
        document.getElementById(this.getWidgetDomId(widget)).innerHTML += this.getMultivalueBadge(
            result,
            caption,
        );
    }
    private renderGroupWidget(widget: any, attr: string, template: any) {
        if (!widget.subwidgets || !widget.subwidgets.length) {
            return 'Widget ' + widget.id + ' is a group widget, but has no subwidgets attached';
        }
        let html = '<div class="widgetGroup">';
        for (let sub of widget.subwidgets) {
            let subwidget = this.getWidget(sub.id);
            if (subwidget == null) {
                html += 'Widget ' + sub.id + ' was not found. Check the widget id';
            } else if (this.isMultivalueWidget(subwidget)) {
                html +=
                    'Widget ' +
                    subwidget.id +
                    ' is a multivalue widget. This is not supported for groups';
            } else {
                let render = this.renderWidget(subwidget, null, template);
                html += render ? render : '';
            }
        }
        html +=
            `<div class="widgetGroupAdd"><div class="btn waves-effect waves-light" onclick="` +
            this.getWindowComponent() +
            `.addGroupValues('` +
            widget.id +
            `')">` +
            this.translate.instant('ADD') +
            `</div></div></div>
            <div id="` +
            this.getWidgetDomId(widget) +
            `" class="multivalueBadges"></div>`;
        return html;
    }
    private renderTemplateWidget(widget: any) {
        if (this.uiService.isMobile()) return '';
        let html =
            `<div class="mdsTemplate">
                    <a class="clickable templateLink" onclick="` +
            this.getWindowComponent() +
            `.openTemplateDialog();">` +
            this.translate.instant('MDS.TEMPLATE_LINK') +
            ` <i class="material-icons">arrow_forward</i></a>
                </div>`;
        return html;
    }
    private renderLicense(widget: any) {
        const constrain = this.handleWidgetConstrains(widget, {
            requiresNode: false,
            supportsBulk: false,
        });
        if (constrain) {
            return constrain;
        }
        if (this.mode == 'search') {
            if (!widget.values) {
                return "widget 'license' does not have values connected, can't render it.";
            }
            for (let value of widget.values) {
                let image = this.nodeHelper.getLicenseIconByString(value.id, false);
                if (image) value.imageSrc = image;
            }
            widget.type = 'checkboxVertical';
            let html = this.renderCheckboxWidget(widget, null, true);
            return html;
        } else {
            const constrain = this.handleWidgetConstrains(widget, {
                requiresNode: true,
                supportsBulk: false,
            });
            if (constrain) {
                return constrain;
            }
            let html = `<div class="mdsLicense">`;
            let isSafe =
                this.connector.getCurrentLogin() &&
                this.connector.getCurrentLogin().currentScope != null;
            let canDelete =
                this.currentNodes[0] &&
                this.currentNodes[0].access.indexOf(RestConstants.ACCESS_DELETE) != -1;
            if (
                isSafe ||
                !this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_LICENSE)
            ) {
                html +=
                    `<div class="mdsNoPermissions">` +
                    this.translate.instant('MDS.LICENSE_NO_PERMISSIONS' + (isSafe ? '_SAFE' : '')) +
                    `</div>`;
            } else if (!canDelete) {
                html +=
                    `<div class="mdsNoPermissions">` +
                    this.translate.instant('MDS.LICENSE_NO_PERMISSIONS_MATERIAL') +
                    `</div>`;
            } else {
                html +=
                    `<a class="clickable licenseLink" onclick="` +
                    this.getWindowComponent() +
                    `.openLicenseDialog();">` +
                    this.translate.instant('MDS.LICENSE_LINK') +
                    ` <i aria-hidden="true" class="material-icons">arrow_forward</i></a>`;
            }
            html += `</div>`;
            return html;
        }
    }
    private renderWorkflow(widget: any) {
        if (this.mode == 'search') {
            let workflows = this.nodeHelper.getWorkflows();
            widget.values = [];
            for (let w of workflows) {
                let value: any = {};
                value.id = w.id;
                value.caption =
                    '<div class="mds-workflow-status" style="background-color:' +
                    w.color +
                    '"></div>' +
                    this.translate.instant('WORKFLOW.' + w.id);
                widget.values.push(value);
            }
            widget.type = 'checkboxVertical';
            let html = this.renderCheckboxWidget(widget, null, true);
            return html;
        } else {
            return "widget 'workflow' is not supported in this mode.";
        }
    }
    private setPreview(counter = 1) {
        let preview: any = document.getElementById(this.getDomId('preview'));
        if (preview) {
            if (!this.currentNodes) {
                if (this.createType == MdsType.ToolDefinition) {
                    preview.src = this.connector.getThemeMimePreview('tool_definition.svg');
                } else {
                    preview.src = this.connector.getThemeMimePreview('file.svg');
                }
                return;
            }
            if (preview.src && !preview.src.startsWith(this.currentNodes[0].preview.url)) return;
            preview.src =
                this.currentNodes[0].preview.url +
                '&crop=true&width=400&height=300&dontcache=' +
                new Date().getMilliseconds();
            //if(node.preview.isIcon){
            setTimeout(() => {
                //this.node.getNodeMetadata(node.ref.id).subscribe((data:NodeWrapper)=>{this.setPreview(data.node)});
                this.setPreview(counter * 2);
            }, Math.min(10000, 500 * counter));
            //}
        }
    }

    private setGeneralNodeData() {
        setTimeout(() => {
            this.setPreview();
            this.setCurrentChildobjects();
        }, 10);
    }

    private onUpdatePreview(callback: () => void = null) {
        let preview = null;
        let remove = false;
        try {
            remove =
                document.getElementById(this.getDomId('preview-deleted')).style.display != 'none';
            preview = (document.getElementById(this.getDomId('preview-select')) as any).files[0];
        } catch (e) {}
        if (remove) {
            this.node.deleteNodePreview(this.currentNodes[0].ref.id).subscribe(
                () => {
                    this.onAddChildobject(callback);
                },
                (error: any) => {
                    this.toast.error(error);
                    this.globalProgress = false;
                },
            );
        } else if (preview) {
            this.node.uploadNodePreview(this.currentNodes[0].ref.id, preview).subscribe(
                () => {
                    this.onAddChildobject(callback);
                },
                (error: any) => {
                    this.toast.error(error);
                    this.globalProgress = false;
                },
            );
        } else {
            this.onAddChildobject(callback);
        }
    }
    cancel() {
        this.onCancel.emit();
    }

    private applySuggestions() {
        setTimeout(() => {
            if (!this.currentWidgets) return;
            let values = this.getValues([], false);
            for (let property in this._suggestions) {
                let widget: any = null;
                for (let w of this.currentWidgets) {
                    if (w.id == property) widget = w;
                }
                let element = document.getElementById(
                    this.getWidgetDomId(widget) + '_badgeSuggestions',
                );
                if (element) {
                    element.style.display = '';
                    element.innerHTML = '';
                    for (let item of this._suggestions[property]) {
                        if (Helper.indexOfNoCase(values[property], item.id) == -1) {
                            element.innerHTML += this.getSuggestBadge(
                                item.id,
                                item.caption,
                                this.getWidgetDomId(widget),
                            );
                        }
                    }
                } else {
                    //console.log("no suggestion area found for widget " + property);
                }
                if (!widget) {
                    //console.warn("no widget found for " + property);
                }
            }
        });
    }

    getWidget(id: string, template: string = null, widgets = this.mds.widgets) {
        return MdsHelper.getWidgetWithCondition(
            this.connector,
            this.getCurrentProperties(),
            id,
            template,
            widgets,
        );
    }

    private getKeyCaption(key: string, values: any[]) {
        if (!values) return key;

        for (let value of values) {
            if (value.id == key) return value.caption;
        }
        return key;
    }

    private isContentEditable() {
        let editor =
            this.currentNodes &&
            this.currentNodes[0].properties[RestConstants.CCM_PROP_EDITOR_TYPE];
        let wwwurl =
            this.currentNodes && this.currentNodes[0].properties[RestConstants.CCM_PROP_IO_WWWURL];
        return editor !== 'tinymce' && !wwwurl;
    }

    private isExtendedWidget(widget: any) {
        return (
            widget.isExtended == true ||
            widget.extended == true ||
            widget.isExtended == 'true' ||
            widget.extended == 'true'
        );
    }
    private isRequiredWidget(widget: any) {
        return (
            widget.isRequired == true ||
            widget.required == true ||
            widget.isRequired == 'true' ||
            widget.required == 'true'
        );
    }
    private highlightSearch(caption: string, searchString: string): string {
        let pos = caption.toLowerCase().indexOf(searchString.toLowerCase());
        if (pos == -1) return caption;
        return (
            caption.substring(0, pos) +
            '<span class=suggestHighlight>' +
            caption.substring(pos, pos + searchString.length) +
            '</span>' +
            caption.substring(pos + searchString.length)
        );
    }

    private setActiveAuthor(type: number) {
        this.activeAuthorType = type;
        let freetext = document.getElementById(this.getDomId('mdsAuthorFreetext'));
        let person = document.getElementById(this.getDomId('mdsAuthorPerson'));
        if (!freetext || !person) return;
        let tabs = document
            .getElementById(this.getDomId('mdsAuthorTabs'))
            .getElementsByTagName('li');
        freetext.style.display = 'none';
        person.style.display = 'none';
        for (let i = 0; i < tabs.length; i++) {
            tabs[i].getElementsByTagName('a')[0].className = tabs[i]
                .getElementsByTagName('a')[0]
                .className.replace('active', '')
                .trim();
        }
        tabs[type].getElementsByTagName('a')[0].className += ' active';
        if (type == MdsComponent.AUTHOR_TYPE_FREETEXT) {
            freetext.style.display = '';
        }
        if (type == MdsComponent.AUTHOR_TYPE_PERSON) {
            person.style.display = '';
        }
    }

    private addAuthorValue(properties: any) {
        if (
            document.getElementById(this.getDomId(RestConstants.CCM_PROP_AUTHOR_FREETEXT)) ||
            document.getElementById(
                this.getDomId(RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR),
            )
        ) {
            //if(this.activeAuthorType==MdsComponent.AUTHOR_TYPE_FREETEXT)
            if (
                Helper.indexOfObjectArray(
                    this.currentWidgets,
                    'id',
                    RestConstants.CCM_PROP_AUTHOR_FREETEXT,
                ) == -1
            ) {
                this.currentWidgets.push({
                    id: RestConstants.CCM_PROP_AUTHOR_FREETEXT,
                    type: 'textarea',
                });
            }
            //if(this.activeAuthorType==MdsComponent.AUTHOR_TYPE_PERSON)
            if (
                Helper.indexOfObjectArray(
                    this.currentWidgets,
                    'id',
                    RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR,
                ) == -1
            ) {
                this.currentWidgets.push({
                    id: RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR,
                    type: 'vcard',
                });
            }
        }
    }

    private setRenderedHtml(html: string) {
        if (!html.trim()) this.rendered = null;
        else this.rendered = this.sanitizer.bypassSecurityTrustHtml(html);
    }

    private isPrimitiveWidget(widget: any) {
        return (
            widget.type == 'text' ||
            widget.type == 'number' ||
            widget.type == 'email' ||
            widget.type == 'date' ||
            widget.type == 'month' ||
            widget.type == 'color'
        );
    }

    private htmlEscape(caption: string) {
        return caption.split('"').join('&quot;');
    }

    private replaceVariables(widget: any) {
        if (this.variables == null) return;
        widget.caption = this.replaceVariableString(widget.caption, this.variables);
        widget.placeholder = this.replaceVariableString(widget.placeholder, this.variables);
        widget.icon = this.replaceVariableString(widget.icon, this.variables);
        widget.defaultvalue = this.replaceVariableString(widget.defaultvalue, this.variables);
    }

    private replaceVariableString(string: string, variables: string[]) {
        if (!string) return string;
        if (!string.match('\\${.+}')) {
            return string;
        }
        for (let key in variables) {
            if ('${' + key + '}' == string) {
                return variables[key];
            }
        }
        console.warn(
            'mds declared variable ' +
                string +
                ', but it was not found in the config variables. List of known variables below',
        );
        console.warn(variables);
        return string;
    }
    private finish() {
        this.onDone.emit(this.currentNodes);
        this.globalProgress = false;
        this.toast.toast('WORKSPACE.EDITOR.UPDATED');
    }
    private getRemovedChildobjects() {
        let list = [];
        for (let node of this.currentChildobjects) {
            let removed = true;
            for (let childs of this.childobjects) {
                if (childs.node && Helper.objectEquals(childs.node.ref, node.ref)) {
                    removed = false;
                    break;
                }
            }
            if (removed) list.push(node);
        }
        return list;
    }
    private onRemoveChildobject(callback: () => void = null, pos = 0) {
        if (pos >= this.getRemovedChildobjects().length) {
            if (callback) callback();
            this.finish();
            return;
        }
        let child = this.getRemovedChildobjects()[pos];
        this.node.deleteNode(child.ref.id, false).subscribe(() => {
            this.onRemoveChildobject(callback, pos + 1);
        });
    }
    setChildobjectProperties(props: any) {
        let edit = this.editChildobject || this.editChildobjectLicense;
        // keep any existing license data
        if (this.editChildobject && edit.child.properties) {
            for (let key in props) {
                edit.child.properties[key] = props[key];
            }
        } else {
            edit.child.properties = props;
        }
        edit.child.name = props[RestConstants.LOM_PROP_TITLE]
            ? props[RestConstants.LOM_PROP_TITLE]
            : props[RestConstants.CM_NAME];
        this.editChildobject = null;
        this.editChildobjectLicense = null;
        this.refreshChildobjects();
    }

    private getChildobjectProperties(child: any, pos: number = null) {
        let props: any;
        if (child.properties) {
            props = child.properties;
        } else if (child.file) {
            props = RestHelper.createNameProperty(child.name);
        } else {
            console.error('Invalid object state for childobject', child);
            return null;
        }
        props[RestConstants.CCM_PROP_CHILDOBJECT_ORDER] = [pos];
        return props;
    }
    private onAddChildobject(callback: () => void = null, pos = 0) {
        if (pos >= this.childobjects.length) {
            this.onRemoveChildobject(callback);
            return;
        }

        let child = this.childobjects[pos];
        if (child.file) {
            this.node
                .createNode(
                    this.currentNodes[0].ref.id,
                    RestConstants.CCM_TYPE_IO,
                    [RestConstants.CCM_ASPECT_IO_CHILDOBJECT],
                    this.getChildobjectProperties(child, pos),
                    true,
                    '',
                    RestConstants.CCM_ASSOC_CHILDIO,
                )
                .subscribe((data: NodeWrapper) => {
                    this.node
                        .uploadNodeContent(
                            data.node.ref.id,
                            child.file,
                            RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                        )
                        .subscribe(
                            () => {
                                this.onAddChildobject(callback, pos + 1);
                            },
                            (error) => {
                                if (
                                    RestHelper.errorMatchesAny(
                                        error,
                                        RestConstants.CONTENT_QUOTA_EXCEPTION,
                                    )
                                ) {
                                    this.node
                                        .deleteNode(data.node.ref.id, false)
                                        .subscribe(() => {});
                                    this.toast.error(null, 'MDS.ADD_CHILD_OBJECT_QUOTA_REACHED', {
                                        name: child.name,
                                    });
                                    this.globalProgress = false;
                                    return;
                                }
                                if(RestHelper.errorMatchesAny(error,RestConstants.CONTENT_VIRUS_EXCEPTION)){
                                  this.toast.error(null,"MDS.ADD_CHILD_OBJECT_VIRUS_DETECTED",{name:child.name});
                                  this.globalProgress=false;
                                  return;
                                }
                            },
                        );
                });
        } else if (child.link) {
            let properties: any = {};
            properties[RestConstants.CCM_PROP_IO_WWWURL] = [child.link];
            this.node
                .createNode(
                    this.currentNodes[0].ref.id,
                    RestConstants.CCM_TYPE_IO,
                    [RestConstants.CCM_ASPECT_IO_CHILDOBJECT],
                    this.getChildobjectProperties(child, pos),
                    true,
                    RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                    RestConstants.CCM_ASSOC_CHILDIO,
                )
                .subscribe((data: NodeWrapper) => {
                    this.onAddChildobject(callback, pos + 1);
                });
        } else {
            this.node
                .editNodeMetadata(child.node.ref.id, this.getChildobjectProperties(child, pos))
                .subscribe(() => {
                    this.onAddChildobject(callback, pos + 1);
                });
        }
    }

    private getWidgetDomId(widget: any) {
        return this.getDomId(widget.id);
    }
    private getDomId(id: string) {
        return id + '_' + this.mdsId;
    }

    private loadConfig() {
        this.locator.getConfigVariables().subscribe((variables: string[]) => {
            this.variables = variables;
            const node = this.currentNodes[0];
            for (const property in node.properties) {
                this.properties.push(property);
            }
            this.properties.sort();
            let nodeGroup;
            try {
                nodeGroup = this._groupId || this.mdsEditorCommon.getGroupId(this.currentNodes);
            } catch (error) {
                this.toast.error(null, error.message);
                this.cancel();
                return;
            }
            this.renderGroup(nodeGroup, this.mds);
            setTimeout(() => {
                this.initialValues = this.getValues();
            }, 15);
            this.isLoading = false;
        });
    }

    getCurrentProperties() {
        return this.currentNodes ? this.currentNodes[0].properties : this._currentValues;
    }

    private getMergedProperties() {
        if (this.currentNodes && this.currentNodes.length === 1) {
            return this.currentNodes[0].properties;
        }
        let properties: any = {};
        for (const key of Object.keys(this.currentNodes[0].properties)) {
            const values = this.currentNodes
                .map((n) => n.properties[key])
                .reduce((a, b) => {
                    if (Helper.arrayEquals(a, b)) {
                        return a;
                    }
                    const result = [];
                    for (const v of a) {
                        if (b && b.indexOf(v) !== -1) {
                            result.push(v);
                        }
                    }
                    return result;
                });
            if (values.length > 0) {
                properties[key] = values;
            }
        }
        return properties;
    }
}
export enum BulkBehavior {
    Default, // default equals no replace on choose, but show options
    Replace, // Don't display settings, simply replace for all (usefull after uploads)
}
