export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
    ID: string;
    String: string;
    Boolean: boolean;
    Int: number;
    Float: number;
    Color: any;
    Date: any;
    Duration: any;
    Locale: any;
};

export type Affiliation = {
    __typename?: 'Affiliation';
    context?: Maybe<Array<Scalars['String']>>;
    scope?: Maybe<Scalars['String']>;
};

export type Association = {
    __typename?: 'Association';
    forkedOrigin?: Maybe<Metadata>;
    legacy?: Maybe<LegacyAssociation>;
    original?: Maybe<Metadata>;
    publishedOriginal?: Maybe<Metadata>;
    symlink?: Maybe<Metadata>;
};

export type BooleanSuggestionData = SuggestionDataInterface & {
    __typename?: 'BooleanSuggestionData';
    info: SuggestionInfo;
    value: Scalars['Boolean'];
    version?: Maybe<Array<Scalars['String']>>;
};

export type BooleanSuggestionDataInput = {
    info: SuggestionInfoInput;
    value: Scalars['Boolean'];
    version: Array<Scalars['String']>;
};

export type Classification = {
    __typename?: 'Classification';
    description?: Maybe<Scalars['String']>;
    purpose: Scalars['String'];
    taxon?: Maybe<Array<RangedValue>>;
};

export type ClassificationInput = {
    description?: InputMaybe<Scalars['String']>;
    purpose: Scalars['String'];
    taxon?: InputMaybe<Array<RangedValueInput>>;
};

export type ClassificationSuggestion = {
    __typename?: 'ClassificationSuggestion';
    description?: Maybe<StringSuggestionData>;
    purpose?: Maybe<StringSuggestionData>;
    taxon?: Maybe<Array<Maybe<RangedValueSuggestionData>>>;
};

export type ClassificationSuggestionInput = {
    description?: InputMaybe<StringSuggestionDataInput>;
    purpose?: InputMaybe<StringSuggestionDataInput>;
    taxon?: InputMaybe<Array<InputMaybe<RangedValueSuggestionDataInput>>>;
};

export type Collection = {
    __typename?: 'Collection';
    authorFreetext?: Maybe<Scalars['String']>;
    color?: Maybe<Scalars['Color']>;
    level?: Maybe<Scalars['Boolean']>;
    order?: Maybe<Order>;
    pinned?: Maybe<Pinned>;
    position?: Maybe<Scalars['Int']>;
    remote?: Maybe<Metadata>;
    remoteSource?: Maybe<Scalars['String']>;
    scope?: Maybe<Scalars['String']>;
    shortTitle?: Maybe<Scalars['String']>;
    type?: Maybe<Scalars['String']>;
    viewType?: Maybe<Scalars['String']>;
};

export type Contribute = {
    __typename?: 'Contribute';
    content: Array<Scalars['String']>;
    role: Scalars['String'];
};

export type ContributeInput = {
    content: Array<Scalars['String']>;
    role: Scalars['String'];
};

export type DateSuggestionData = SuggestionDataInterface & {
    __typename?: 'DateSuggestionData';
    info: SuggestionInfo;
    value: Scalars['Date'];
    version?: Maybe<Array<Scalars['String']>>;
};

export type DateSuggestionDataInput = {
    info: SuggestionInfoInput;
    value: Scalars['Date'];
    version: Array<Scalars['String']>;
};

export type Dimension = {
    __typename?: 'Dimension';
    height: Scalars['Float'];
    width: Scalars['Float'];
};

export type DimensionInput = {
    height: Scalars['Float'];
    width: Scalars['Float'];
};

export type DimensionSuggestionData = SuggestionDataInterface & {
    __typename?: 'DimensionSuggestionData';
    info: SuggestionInfo;
    value: Dimension;
    version?: Maybe<Array<Scalars['String']>>;
};

export type DimensionSuggestionDataInput = {
    info: SuggestionInfoInput;
    value: DimensionInput;
    version: Array<Scalars['String']>;
};

export type Directory = {
    __typename?: 'Directory';
    hasTemplate?: Maybe<Scalars['Boolean']>;
    type?: Maybe<Scalars['String']>;
};

export type DurationSuggestionData = SuggestionDataInterface & {
    __typename?: 'DurationSuggestionData';
    info: SuggestionInfo;
    value: Scalars['Duration'];
    version?: Maybe<Array<Scalars['String']>>;
};

export type DurationSuggestionDataInput = {
    info: SuggestionInfoInput;
    value: Scalars['Duration'];
    version: Array<Scalars['String']>;
};

export type Editorial = {
    __typename?: 'Editorial';
    checklist?: Maybe<Array<RangedValue>>;
    state?: Maybe<RangedValue>;
};

export type EditorialInput = {
    checklist?: InputMaybe<Array<RangedValueInput>>;
    state?: InputMaybe<RangedValueInput>;
};

export type EditorialSuggestion = {
    __typename?: 'EditorialSuggestion';
    checklist?: Maybe<Array<Maybe<RangedValueSuggestionData>>>;
    state?: Maybe<RangedValueSuggestionData>;
};

export type EditorialSuggestionInput = {
    checklist?: InputMaybe<Array<InputMaybe<RangedValueSuggestionDataInput>>>;
    state?: InputMaybe<RangedValueSuggestionDataInput>;
};

export type Educational = {
    __typename?: 'Educational';
    context?: Maybe<Array<RangedValue>>;
    curriculum?: Maybe<Array<RangedValue>>;
    intendedEndUserRole?: Maybe<Array<RangedValue>>;
    interactivityType?: Maybe<Array<RangedValue>>;
    language?: Maybe<Array<Scalars['String']>>;
    learningResourceType?: Maybe<Array<RangedValue>>;
    typicalAgeRange?: Maybe<Array<RangedValue>>;
    typicalAgeRangeNominal?: Maybe<IntRange>;
    typicalLerningTime?: Maybe<Scalars['Duration']>;
};

export type EducationalInput = {
    context?: InputMaybe<Array<RangedValueInput>>;
    curriculum?: InputMaybe<Array<RangedValueInput>>;
    intendedEndUserRole?: InputMaybe<Array<RangedValueInput>>;
    interactivityType?: InputMaybe<Array<RangedValueInput>>;
    language?: InputMaybe<Array<Scalars['String']>>;
    learningResourceType?: InputMaybe<Array<RangedValueInput>>;
    typicalAgeRange?: InputMaybe<Array<RangedValueInput>>;
    typicalAgeRangeNominal?: InputMaybe<IntRangeInput>;
    typicalLerningTime?: InputMaybe<Scalars['Duration']>;
};

export type EducationalSuggestion = {
    __typename?: 'EducationalSuggestion';
    context?: Maybe<Array<Maybe<RangedValueSuggestionData>>>;
    curriculum?: Maybe<Array<Maybe<RangedValueSuggestionData>>>;
    intendedEndUserRole?: Maybe<Array<Maybe<RangedValueSuggestionData>>>;
    interactivityType?: Maybe<Array<Maybe<RangedValueSuggestionData>>>;
    language?: Maybe<Array<Maybe<StringSuggestionData>>>;
    learningResourceType?: Maybe<Array<Maybe<RangedValueSuggestionData>>>;
    typicalAgeRange?: Maybe<Array<Maybe<RangedValueSuggestionData>>>;
    typicalAgeRangeNominal?: Maybe<IntRangeSuggestionData>;
    typicalLerningTime?: Maybe<DurationSuggestionData>;
};

export type EducationalSuggestionInput = {
    context?: InputMaybe<Array<InputMaybe<RangedValueSuggestionDataInput>>>;
    curriculum?: InputMaybe<Array<InputMaybe<RangedValueSuggestionDataInput>>>;
    intendedEndUserRole?: InputMaybe<Array<InputMaybe<RangedValueSuggestionDataInput>>>;
    interactivityType?: InputMaybe<Array<InputMaybe<RangedValueSuggestionDataInput>>>;
    language?: InputMaybe<Array<InputMaybe<StringSuggestionDataInput>>>;
    learningResourceType?: InputMaybe<Array<InputMaybe<RangedValueSuggestionDataInput>>>;
    typicalAgeRange?: InputMaybe<Array<InputMaybe<RangedValueSuggestionDataInput>>>;
    typicalAgeRangeNominal?: InputMaybe<IntRangeSuggestionDataInput>;
    typicalLerningTime?: InputMaybe<DurationSuggestionDataInput>;
};

export type Format = {
    __typename?: 'Format';
    content?: Maybe<Scalars['String']>;
    mimetype?: Maybe<Scalars['String']>;
    subtype?: Maybe<Array<Scalars['String']>>;
    type?: Maybe<Scalars['String']>;
    version?: Maybe<Scalars['String']>;
};

export type FormatInput = {
    content?: InputMaybe<Scalars['String']>;
    mimetype?: InputMaybe<Scalars['String']>;
    subtype?: InputMaybe<Array<Scalars['String']>>;
    type?: InputMaybe<Scalars['String']>;
    version?: InputMaybe<Scalars['String']>;
};

export type FormatSuggestionData = SuggestionDataInterface & {
    __typename?: 'FormatSuggestionData';
    info: SuggestionInfo;
    value: Format;
    version?: Maybe<Array<Scalars['String']>>;
};

export type FormatSuggestionDataInput = {
    info: SuggestionInfoInput;
    value: FormatInput;
    version: Array<Scalars['String']>;
};

export type General = {
    __typename?: 'General';
    aggregationLevel?: Maybe<RangedValue>;
    coverage?: Maybe<Array<Scalars['String']>>;
    description?: Maybe<Scalars['String']>;
    keyword?: Maybe<Array<Scalars['String']>>;
    language?: Maybe<Array<Scalars['String']>>;
    structure?: Maybe<RangedValue>;
    title?: Maybe<Scalars['String']>;
};

export type GeneralInput = {
    aggregationLevel?: InputMaybe<RangedValueInput>;
    coverage?: InputMaybe<Array<Scalars['String']>>;
    description?: InputMaybe<Array<Scalars['String']>>;
    keyword?: InputMaybe<Array<Scalars['String']>>;
    language?: InputMaybe<Array<Scalars['String']>>;
    structure?: InputMaybe<RangedValueInput>;
    title?: InputMaybe<Scalars['String']>;
};

export type GeneralSuggestion = {
    __typename?: 'GeneralSuggestion';
    aggregationLevel?: Maybe<RangedValueSuggestionData>;
    coverage?: Maybe<Array<Maybe<StringSuggestionData>>>;
    description?: Maybe<StringSuggestionData>;
    keyword?: Maybe<Array<Maybe<StringSuggestionData>>>;
    language?: Maybe<Array<Maybe<StringSuggestionData>>>;
    structure?: Maybe<RangedValueSuggestionData>;
    title?: Maybe<StringSuggestionData>;
};

export type GeneralSuggestionInput = {
    aggregationLevel?: InputMaybe<RangedValueSuggestionDataInput>;
    coverage?: InputMaybe<Array<InputMaybe<StringSuggestionDataInput>>>;
    description?: InputMaybe<StringSuggestionDataInput>;
    keyword?: InputMaybe<Array<InputMaybe<StringSuggestionDataInput>>>;
    language?: InputMaybe<Array<InputMaybe<StringSuggestionDataInput>>>;
    structure?: InputMaybe<RangedValueSuggestionDataInput>;
};

export type ImportedObject = {
    __typename?: 'ImportedObject';
    appId?: Maybe<Scalars['String']>;
    appName?: Maybe<Scalars['String']>;
    nodeId?: Maybe<Scalars['String']>;
};

export type Info = {
    __typename?: 'Info';
    aspects?: Maybe<Array<Scalars['String']>>;
    createDate: Scalars['Date'];
    creator: Scalars['String'];
    filename: Scalars['String'];
    metadataSet?: Maybe<Scalars['String']>;
    modifiedDate: Scalars['Date'];
    modifier: Scalars['String'];
    objectType?: Maybe<RangedValue>;
    organisation?: Maybe<Scalars['String']>;
    originallyCreated?: Maybe<Scalars['Date']>;
    owner?: Maybe<Scalars['String']>;
    preview?: Maybe<Preview>;
    propagateMetadataSet?: Maybe<Scalars['Boolean']>;
    searchContext?: Maybe<Array<Scalars['String']>>;
    url?: Maybe<Scalars['String']>;
    urlOrigin?: Maybe<Scalars['String']>;
};

export type IntRange = {
    __typename?: 'IntRange';
    from?: Maybe<Scalars['Int']>;
    to?: Maybe<Scalars['Int']>;
};

export type IntRangeInput = {
    from?: InputMaybe<Scalars['Int']>;
    to?: InputMaybe<Scalars['Int']>;
};

export type IntRangeSuggestionData = SuggestionDataInterface & {
    __typename?: 'IntRangeSuggestionData';
    info: SuggestionInfo;
    value: IntRange;
    version?: Maybe<Array<Scalars['String']>>;
};

export type IntRangeSuggestionDataInput = {
    info: SuggestionInfoInput;
    value: IntRangeInput;
    version: Array<Scalars['String']>;
};

export type LegacyAssociation = {
    __typename?: 'LegacyAssociation';
    schemaRelation: Array<Scalars['String']>;
};

export type Lifecycle = {
    __typename?: 'Lifecycle';
    contribute?: Maybe<Array<Contribute>>;
    status?: Maybe<RangedValue>;
    version?: Maybe<Scalars['String']>;
};

export type LifecycleInput = {
    contribute?: InputMaybe<Array<ContributeInput>>;
    status?: InputMaybe<RangedValueInput>;
    version?: InputMaybe<Scalars['String']>;
};

export type LocaleSuggestionData = SuggestionDataInterface & {
    __typename?: 'LocaleSuggestionData';
    info: SuggestionInfo;
    value: Scalars['Locale'];
    version?: Maybe<Array<Scalars['String']>>;
};

export type LocaleSuggestionDataInput = {
    info: SuggestionInfoInput;
    value: Scalars['Locale'];
    version: Array<Scalars['String']>;
};

export type Lom = {
    __typename?: 'Lom';
    classification?: Maybe<Array<Classification>>;
    editorial?: Maybe<Array<Editorial>>;
    educational?: Maybe<Array<Educational>>;
    general?: Maybe<General>;
    lifecycle?: Maybe<Lifecycle>;
    metaMetadata?: Maybe<MetaMetadata>;
    rights?: Maybe<Rights>;
    technical?: Maybe<Technical>;
};

export type LomInput = {
    classification?: InputMaybe<Array<ClassificationInput>>;
    editorial?: InputMaybe<Array<EditorialInput>>;
    educational?: InputMaybe<Array<EducationalInput>>;
    general?: InputMaybe<GeneralInput>;
    lifecycle?: InputMaybe<LifecycleInput>;
    metaMetadata?: InputMaybe<MetaMetadataInput>;
    rights?: InputMaybe<RightsInput>;
    technical?: InputMaybe<TechnicalInput>;
};

export type LomSuggestion = {
    __typename?: 'LomSuggestion';
    classification?: Maybe<Array<ClassificationSuggestion>>;
    editorial?: Maybe<Array<EditorialSuggestion>>;
    educational?: Maybe<Array<EducationalSuggestion>>;
    general?: Maybe<GeneralSuggestion>;
    rights?: Maybe<RightsSuggestion>;
    technical?: Maybe<TechnicalSuggestion>;
};

export type LomSuggestionInput = {
    classification?: InputMaybe<Array<InputMaybe<ClassificationSuggestionInput>>>;
    editorial?: InputMaybe<Array<InputMaybe<EditorialSuggestionInput>>>;
    educational?: InputMaybe<Array<InputMaybe<EducationalSuggestionInput>>>;
    general?: InputMaybe<GeneralSuggestionInput>;
    rights?: InputMaybe<RightsSuggestionInput>;
    technical?: InputMaybe<TechnicalSuggestionInput>;
};

export type MetaMetadata = {
    __typename?: 'MetaMetadata';
    contribute?: Maybe<Array<Contribute>>;
    schema?: Maybe<Scalars['String']>;
};

export type MetaMetadataInput = {
    contribute?: InputMaybe<Array<ContributeInput>>;
    schema?: InputMaybe<Scalars['String']>;
};

export type Metadata = {
    __typename?: 'Metadata';
    affiliation?: Maybe<Affiliation>;
    association?: Maybe<Association>;
    collection?: Maybe<Collection>;
    content?: Maybe<Scalars['String']>;
    directory?: Maybe<Directory>;
    generated?: Maybe<Array<Suggestion>>;
    id: Scalars['ID'];
    importedObject?: Maybe<ImportedObject>;
    info?: Maybe<Info>;
    lom?: Maybe<Lom>;
    nodeType: Scalars['String'];
    permission?: Maybe<Permission>;
    published?: Maybe<Published>;
    reference?: Maybe<Reference>;
    remote?: Maybe<Remote>;
    savedSearch?: Maybe<SavedSearch>;
    share?: Maybe<Share>;
    store?: Maybe<Store>;
    version?: Maybe<Version>;
    workflow?: Maybe<Workflow>;
};

export type MetadataFilter = {
    ids?: InputMaybe<Array<Scalars['ID']>>;
};

export type Mutation = {
    __typename?: 'Mutation';
    addOrUpdateSuggestion: Scalars['Boolean'];
    removeSuggestion: Scalars['Boolean'];
};

export type MutationAddOrUpdateSuggestionArgs = {
    suggestion?: InputMaybe<SuggestionInput>;
};

export type MutationRemoveSuggestionArgs = {
    nodeId: Scalars['ID'];
    suggestionId: Scalars['ID'];
};

export type Order = {
    __typename?: 'Order';
    collection?: Maybe<Array<OrderMode>>;
    reference?: Maybe<Array<OrderMode>>;
};

export enum OrderDirection {
    Asc = 'asc',
    Dsc = 'dsc',
}

export type OrderMode = {
    __typename?: 'OrderMode';
    active: Scalars['String'];
    direction: OrderDirection;
};

export type Permission = {
    __typename?: 'Permission';
    action: Scalars['String'];
    history?: Maybe<Array<Scalars['String']>>;
    invited?: Maybe<Array<Scalars['String']>>;
    modified: Scalars['Date'];
    users?: Maybe<Array<Scalars['String']>>;
};

export type Pinned = {
    __typename?: 'Pinned';
    position?: Maybe<Scalars['Int']>;
    status?: Maybe<Scalars['Boolean']>;
};

export type Preview = {
    __typename?: 'Preview';
    data?: Maybe<Scalars['String']>;
    mimetype?: Maybe<Scalars['String']>;
    type?: Maybe<PreviewType>;
    url?: Maybe<Scalars['String']>;
};

export enum PreviewType {
    TypeDefault = 'TYPE_DEFAULT',
    TypeExternal = 'TYPE_EXTERNAL',
    TypeGenerated = 'TYPE_GENERATED',
    TypeUserdefined = 'TYPE_USERDEFINED',
}

export type Published = {
    __typename?: 'Published';
    date?: Maybe<Scalars['Date']>;
    handleId?: Maybe<Scalars['String']>;
    mode?: Maybe<Scalars['String']>;
};

export type Query = {
    __typename?: 'Query';
    metadata?: Maybe<Metadata>;
    metadatas: Array<Maybe<Metadata>>;
};

export type QueryMetadataArgs = {
    id: Scalars['ID'];
    version?: InputMaybe<Scalars['String']>;
};

export type QueryMetadatasArgs = {
    input: MetadataFilter;
};

export type RangedValue = {
    __typename?: 'RangedValue';
    id?: Maybe<Scalars['String']>;
    value: Scalars['String'];
};

export type RangedValueInput = {
    id?: InputMaybe<Scalars['String']>;
    value: Scalars['String'];
};

export type RangedValueSuggestionData = SuggestionDataInterface & {
    __typename?: 'RangedValueSuggestionData';
    info: SuggestionInfo;
    value: RangedValue;
    version?: Maybe<Array<Scalars['String']>>;
};

export type RangedValueSuggestionDataInput = {
    info: SuggestionInfoInput;
    value: RangedValueInput;
    version: Array<Scalars['String']>;
};

export type Reference = {
    __typename?: 'Reference';
    collection?: Maybe<Metadata>;
    description?: Maybe<Scalars['String']>;
    proposalStatus?: Maybe<Scalars['String']>;
    title?: Maybe<Scalars['String']>;
    version?: Maybe<Scalars['String']>;
    videoVtt?: Maybe<Scalars['String']>;
};

export type Remote = {
    id: Scalars['String'];
    repository: Repository;
};

export type RemoteShadow = Remote & {
    __typename?: 'RemoteShadow';
    id: Scalars['String'];
    repository: Repository;
};

export type Replication = Remote & {
    __typename?: 'Replication';
    hash?: Maybe<Scalars['String']>;
    id: Scalars['String'];
    importBlocked: Scalars['Boolean'];
    modified?: Maybe<Scalars['Date']>;
    repository: Repository;
    timestamp?: Maybe<Scalars['String']>;
    uuid?: Maybe<Scalars['String']>;
};

export type Repository = {
    __typename?: 'Repository';
    id: Scalars['String'];
    origin?: Maybe<Scalars['String']>;
    type?: Maybe<Scalars['String']>;
};

export type Rights = {
    __typename?: 'Rights';
    author?: Maybe<Array<Scalars['String']>>;
    copyrightAndOtherRestrictions?: Maybe<RangedValue>;
    cost?: Maybe<RangedValue>;
    description?: Maybe<Scalars['String']>;
    expirationDate?: Maybe<Scalars['Date']>;
    internal?: Maybe<Array<Scalars['String']>>;
    locale?: Maybe<Scalars['Locale']>;
    negotiationPermitted?: Maybe<Scalars['Boolean']>;
    publicAccess?: Maybe<Scalars['Boolean']>;
    restrictedAccess?: Maybe<Scalars['Boolean']>;
    version?: Maybe<Scalars['String']>;
};

export type RightsInput = {
    author?: InputMaybe<Array<Scalars['String']>>;
    copyrightAndOtherRestrictions?: InputMaybe<RangedValueInput>;
    cost?: InputMaybe<RangedValueInput>;
    description?: InputMaybe<Scalars['String']>;
    expirationDate?: InputMaybe<Scalars['Date']>;
    internal?: InputMaybe<Array<Scalars['String']>>;
    locale?: InputMaybe<Scalars['Locale']>;
    negotiationPermitted?: InputMaybe<Scalars['Boolean']>;
    publicAccess?: InputMaybe<Scalars['Boolean']>;
    restrictedAccess?: InputMaybe<Scalars['Boolean']>;
    version?: InputMaybe<Scalars['String']>;
};

export type RightsSuggestion = {
    __typename?: 'RightsSuggestion';
    author?: Maybe<Array<Maybe<StringSuggestionData>>>;
    copyrightAndOtherRestrictions?: Maybe<RangedValueSuggestionData>;
    cost?: Maybe<RangedValueSuggestionData>;
    description?: Maybe<StringSuggestionData>;
    expirationDate?: Maybe<DateSuggestionData>;
    internal?: Maybe<Array<Maybe<StringSuggestionData>>>;
    locale?: Maybe<LocaleSuggestionData>;
    negotiationPermitted?: Maybe<BooleanSuggestionData>;
    publicAccess?: Maybe<BooleanSuggestionData>;
    restrictedAccess?: Maybe<BooleanSuggestionData>;
    version?: Maybe<StringSuggestionData>;
};

export type RightsSuggestionInput = {
    author?: InputMaybe<Array<InputMaybe<StringSuggestionDataInput>>>;
    copyrightAndOtherRestrictions?: InputMaybe<RangedValueSuggestionDataInput>;
    cost?: InputMaybe<RangedValueSuggestionDataInput>;
    description?: InputMaybe<StringSuggestionDataInput>;
    expirationDate?: InputMaybe<DateSuggestionDataInput>;
    internal?: InputMaybe<Array<InputMaybe<StringSuggestionDataInput>>>;
    locale?: InputMaybe<LocaleSuggestionDataInput>;
    negotiationPermitted?: InputMaybe<BooleanSuggestionDataInput>;
    publicAccess?: InputMaybe<BooleanSuggestionDataInput>;
    restrictedAccess?: InputMaybe<BooleanSuggestionDataInput>;
    version?: InputMaybe<StringSuggestionDataInput>;
};

export type SavedSearch = {
    __typename?: 'SavedSearch';
    mds?: Maybe<Scalars['String']>;
    parameters?: Maybe<Array<Scalars['String']>>;
    query?: Maybe<Scalars['String']>;
    repository?: Maybe<Scalars['String']>;
};

export type Share = {
    __typename?: 'Share';
    date: Scalars['Int'];
    downloadCount: Scalars['Int'];
    mail?: Maybe<Scalars['String']>;
    token: Scalars['String'];
};

export type Store = {
    __typename?: 'Store';
    dbId: Scalars['Int'];
    id: Scalars['String'];
    protocol?: Maybe<Scalars['String']>;
};

export type StringSuggestionData = SuggestionDataInterface & {
    __typename?: 'StringSuggestionData';
    info: SuggestionInfo;
    value: Scalars['String'];
    version?: Maybe<Array<Scalars['String']>>;
};

export type StringSuggestionDataInput = {
    info: SuggestionInfoInput;
    value: Scalars['String'];
    version: Array<Scalars['String']>;
};

export type Suggestion = {
    __typename?: 'Suggestion';
    date: Scalars['Date'];
    id: Scalars['String'];
    lom: LomSuggestion;
    nodeId: Scalars['String'];
    type: SuggestionType;
};

export type SuggestionDataInterface = {
    info: SuggestionInfo;
    version?: Maybe<Array<Scalars['String']>>;
};

export type SuggestionInfo = {
    __typename?: 'SuggestionInfo';
    date: Scalars['Date'];
    editor?: Maybe<Scalars['String']>;
    status: SuggestionStatus;
};

export type SuggestionInfoInput = {
    status: SuggestionStatus;
};

export type SuggestionInput = {
    id: Scalars['String'];
    lom: LomSuggestionInput;
    nodeId: Scalars['String'];
    type: SuggestionType;
};

export enum SuggestionStatus {
    Accepted = 'ACCEPTED',
    Declined = 'DECLINED',
    Pending = 'PENDING',
}

export enum SuggestionType {
    Ai = 'AI',
    UserProposal = 'USER_PROPOSAL',
}

export type Technical = {
    __typename?: 'Technical';
    dimension?: Maybe<Dimension>;
    duration?: Maybe<Scalars['Duration']>;
    format?: Maybe<Array<Format>>;
    installationRemarks?: Maybe<Array<Scalars['String']>>;
    location?: Maybe<Array<Scalars['String']>>;
    otherPlatformRequirements?: Maybe<Array<Scalars['String']>>;
    size?: Maybe<Scalars['String']>;
};

export type TechnicalInput = {
    dimension?: InputMaybe<DimensionInput>;
    duration?: InputMaybe<Scalars['Duration']>;
    format?: InputMaybe<Array<FormatInput>>;
    installationRemarks?: InputMaybe<Array<Scalars['String']>>;
    location?: InputMaybe<Array<Scalars['String']>>;
    otherPlatformRequirements?: InputMaybe<Array<Scalars['String']>>;
    size?: InputMaybe<Scalars['String']>;
};

export type TechnicalSuggestion = {
    __typename?: 'TechnicalSuggestion';
    dimension?: Maybe<DimensionSuggestionData>;
    duration?: Maybe<DurationSuggestionData>;
    format?: Maybe<Array<Maybe<FormatSuggestionData>>>;
    installationRemarks?: Maybe<Array<Maybe<StringSuggestionData>>>;
    location?: Maybe<Array<Maybe<StringSuggestionData>>>;
    otherPlatformRequirements?: Maybe<Array<Maybe<StringSuggestionData>>>;
    size?: Maybe<StringSuggestionData>;
};

export type TechnicalSuggestionInput = {
    dimension?: InputMaybe<DimensionSuggestionDataInput>;
    duration?: InputMaybe<DurationSuggestionDataInput>;
    format?: InputMaybe<Array<InputMaybe<FormatSuggestionDataInput>>>;
    installationRemarks?: InputMaybe<Array<InputMaybe<StringSuggestionDataInput>>>;
    location?: InputMaybe<Array<InputMaybe<StringSuggestionDataInput>>>;
    otherPlatformRequirements?: InputMaybe<Array<InputMaybe<StringSuggestionDataInput>>>;
    size?: InputMaybe<StringSuggestionDataInput>;
};

export type Version = {
    __typename?: 'Version';
    autoCreateVersion?: Maybe<Scalars['Boolean']>;
    comment?: Maybe<Scalars['String']>;
    type?: Maybe<Scalars['String']>;
    version?: Maybe<Scalars['String']>;
};

export type Workflow = {
    __typename?: 'Workflow';
    instructions?: Maybe<Scalars['String']>;
    protocol?: Maybe<Array<Scalars['String']>>;
    receiver?: Maybe<Array<Scalars['String']>>;
    status?: Maybe<Scalars['String']>;
};
